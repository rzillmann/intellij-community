// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.debugger.streams.psi.impl;

import com.intellij.debugger.streams.core.trace.impl.handler.type.GenericType;
import com.intellij.debugger.streams.core.wrapper.CallArgument;
import com.intellij.debugger.streams.core.wrapper.IntermediateStreamCall;
import com.intellij.debugger.streams.core.wrapper.StreamChain;
import com.intellij.debugger.streams.core.wrapper.TerminatorStreamCall;
import com.intellij.debugger.streams.core.wrapper.impl.*;
import com.intellij.debugger.streams.psi.ChainTransformer;
import com.intellij.debugger.streams.trace.dsl.impl.java.JavaTypes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class JavaChainTransformerImpl implements ChainTransformer.Java {
  @Override
  public @NotNull StreamChain transform(@NotNull List<PsiMethodCallExpression> streamExpressions,
                                        @NotNull PsiElement context) {
    final PsiMethodCallExpression firstCall = streamExpressions.get(0);

    final PsiExpression qualifierExpression = firstCall.getMethodExpression().getQualifierExpression();
    final PsiType qualifierType = qualifierExpression == null ? null : qualifierExpression.getType();
    final GenericType typeAfterQualifier = qualifierType == null
                                           ? getGenericTypeOfThis(qualifierExpression)
                                           : JavaTypes.INSTANCE.fromStreamPsiType(qualifierType);
    final QualifierExpressionImpl qualifier =
      qualifierExpression == null
      ? new QualifierExpressionImpl("", TextRange.EMPTY_RANGE, typeAfterQualifier)
      : new QualifierExpressionImpl(qualifierExpression.getText(), qualifierExpression.getTextRange(), typeAfterQualifier);
    final List<IntermediateStreamCall> intermediateCalls =
      createIntermediateCalls(typeAfterQualifier, streamExpressions.subList(0, streamExpressions.size() - 1));

    final GenericType typeBefore =
      intermediateCalls.isEmpty() ? qualifier.getTypeAfter() : intermediateCalls.get(intermediateCalls.size() - 1).getTypeAfter();
    final TerminatorStreamCall terminationCall = createTerminationCall(typeBefore, streamExpressions.get(streamExpressions.size() - 1));

    return new StreamChainImpl(qualifier, intermediateCalls, terminationCall, context);
  }

  private static @NotNull GenericType getGenericTypeOfThis(PsiExpression expression) {
    final PsiClass klass = PsiUtil.getContainingClass(expression);

    return klass == null ? JavaTypes.INSTANCE.getANY()
                         : JavaTypes.INSTANCE.fromPsiClass(klass);
  }

  private static @NotNull List<IntermediateStreamCall> createIntermediateCalls(@NotNull GenericType producerAfterType,
                                                                               @NotNull List<PsiMethodCallExpression> expressions) {
    final List<IntermediateStreamCall> result = new ArrayList<>();

    GenericType typeBefore = producerAfterType;
    for (final PsiMethodCallExpression callExpression : expressions) {
      final String name = resolveMethodName(callExpression);
      final List<CallArgument> args = resolveArguments(callExpression);
      final GenericType type = resolveType(callExpression);
      result.add(new IntermediateStreamCallImpl(name, "", args, typeBefore, type, callExpression.getTextRange()));
      typeBefore = type;
    }

    return result;
  }

  private static @NotNull TerminatorStreamCall createTerminationCall(@NotNull GenericType typeBefore, @NotNull PsiMethodCallExpression expression) {
    final String name = resolveMethodName(expression);
    final List<CallArgument> args = resolveArguments(expression);
    final GenericType resultType = resolveTerminationCallType(expression);
    return new TerminatorStreamCallImpl(name, "", args, typeBefore, resultType, expression.getTextRange(), resultType.equals(JavaTypes.INSTANCE.getVOID()));
  }

  private static @NotNull List<CallArgument> resolveArguments(@NotNull PsiMethodCallExpression methodCall) {
    final PsiExpressionList list = methodCall.getArgumentList();
    return StreamEx.of(list.getExpressions())
      .zipWith(StreamEx.of(list.getExpressionTypes()),
               (expression, type) -> new CallArgumentImpl(GenericsUtil.getVariableTypeByExpressionType(type).getCanonicalText(),
                                                          expression.getText()))
      .collect(Collectors.toList());
  }

  private static @NotNull String resolveMethodName(@NotNull PsiMethodCallExpression methodCall) {
    final String name = methodCall.getMethodExpression().getReferenceName();
    Objects.requireNonNull(name, "Method reference must be not null" + methodCall.getText());
    return name;
  }

  private static @NotNull PsiType extractType(@NotNull PsiMethodCallExpression expression) {
    final PsiType returnType = expression.getType();
    Objects.requireNonNull(returnType, "Method return type must be not null" + expression.getText());
    return returnType;
  }

  private static @NotNull GenericType resolveType(@NotNull PsiMethodCallExpression call) {
    return JavaTypes.INSTANCE.fromStreamPsiType(extractType(call));
  }

  private static @NotNull GenericType resolveTerminationCallType(@NotNull PsiMethodCallExpression call) {
    return JavaTypes.INSTANCE.fromPsiType(extractType(call));
  }
}
