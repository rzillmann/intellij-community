// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.completion.builder;

import org.jetbrains.plugins.groovy.completion.CompletionResult;

public class GrExternalBuilderStrategyWithJavaClassTest extends GrBuilderTransformationCompletionTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addClass("""
                         public class Pojo {
                             String name;
                             Object dynamic;
                             int counter;
                             public Object someFieldWithoutSetter;
                         
                             public void setName(String name) {}
                             public void setDynamic(Object dynamic) {}
                             public void setCounter(int counter) {}
                             public void setWithoutField(int a) {}
                             private void setPrivate(Object a) {}
                             void setPackageLocal(Object a) {}
                         }
                         """);
  }

  public void test_no_prefix() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo)
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_empty_prefix() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, prefix = '')
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_custom_prefix() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, prefix = 'customPrefix')
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "customPrefixName", "customPrefixDynamic", "customPrefixCounter", "customPrefixWithoutField");
  }

  public void test_spaces_prefix() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, prefix = '   ')
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "   Name", "   Dynamic", "   Counter", "   WithoutField");
  }

  public void test_null_prefix() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, prefix = null)
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_custom_build_method() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, buildMethodName = 'customBuild')
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "customBuild", "name", "dynamic", "counter", "withoutField");
  }

  public void test_null_build_method() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, buildMethodName = null)
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_empty_build_method() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, buildMethodName = '')
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_spaces_build_method() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo, buildMethodName = '    ')
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, "    ", "name", "dynamic", "counter", "withoutField");
  }

  public void test_next_setter() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo)
                       class PojoBuilder {}
                       
                       new PojoBuilder().counter(1).<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_one_more_setter_further() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo)
                       class PojoBuilder {}
                       
                       new PojoBuilder().counter(1).name("Janet").<caret>
                       """, "build", "name", "dynamic", "counter", "withoutField");
  }

  public void test_only_java_bean_properties() {
    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Pojo)
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, CompletionResult.notContain, "private", "packageLocal", "someFieldWithoutSetter");
  }

  public void test_not_include_super_properties() {
    myFixture.addClass("""
                         class Child extends Pojo {
                           String secondName;
                           public void setSecondName(String secondName) {}
                         }
                         """);

    String code = """
      import groovy.transform.builder.Builder
      import groovy.transform.builder.ExternalStrategy
      
      @Builder(builderStrategy = ExternalStrategy, forClass = Child)
      class PojoBuilder {}
      
      new PojoBuilder().<caret>
      """;
    doCompletionTest(code, CompletionResult.contain, "secondName");
    doCompletionTest(code, CompletionResult.notContain, "name", "dynamic", "counter", "withoutField");
  }

  public void test_include_super_properties() {
    myFixture.addClass("""
                         class Child extends Pojo {
                           String secondName;
                           public void setSecondName(String secondName) {}
                         }
                         """);

    doCompletionTest("""
                       import groovy.transform.builder.Builder
                       import groovy.transform.builder.ExternalStrategy
                       
                       @Builder(builderStrategy = ExternalStrategy, forClass = Child, includeSuperProperties = true)
                       class PojoBuilder {}
                       
                       new PojoBuilder().<caret>
                       """, CompletionResult.contain, "secondName", "name", "dynamic", "counter", "withoutField");
  }
}
