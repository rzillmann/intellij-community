
/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.generation.surroundWith;

import com.intellij.java.JavaBundle;
import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class JavaWithDoWhileSurrounder extends JavaStatementsModCommandSurrounder {
  @Override
  public String getTemplateDescription() {
    return JavaBundle.message("surround.with.dowhile.template");
  }

  @Override
  protected void surroundStatements(@NotNull ActionContext context,
                                    @NotNull PsiElement container,
                                    @NotNull PsiElement @NotNull [] statements,
                                    @NotNull ModPsiUpdater updater) throws IncorrectOperationException {
    Project project = context.project();
    PsiManager manager = PsiManager.getInstance(project);
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(manager.getProject());
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

    statements = SurroundWithUtil.moveDeclarationsOut(container, statements, false);
    if (statements.length == 0) return;

    @NonNls String text = "do{\n}while(true);";
    PsiDoWhileStatement doWhileStatement = (PsiDoWhileStatement)factory.createStatementFromText(text, null);
    doWhileStatement = (PsiDoWhileStatement)codeStyleManager.reformat(doWhileStatement);

    doWhileStatement = (PsiDoWhileStatement)addAfter(doWhileStatement, container, statements);

    PsiStatement body = doWhileStatement.getBody();
    if (!(body instanceof PsiBlockStatement block)) return;
    PsiCodeBlock bodyBlock = block.getCodeBlock();
    SurroundWithUtil.indentCommentIfNecessary(bodyBlock, statements);
    addRangeWithinContainer(bodyBlock, container, statements, false);
    container.deleteChildRange(statements[0], statements[statements.length - 1]);

    PsiExpression condition = doWhileStatement.getCondition();
    if (condition != null) {
      updater.select(condition);
    }
  }
}