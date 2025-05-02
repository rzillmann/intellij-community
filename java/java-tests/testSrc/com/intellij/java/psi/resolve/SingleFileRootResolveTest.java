// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.psi.resolve;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class SingleFileRootResolveTest extends LightJavaCodeInsightFixtureTestCase {
  private final @NotNull LightProjectDescriptor MY_DESCRIPTOR = new LightProjectDescriptor() {
    @Override
    protected void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
      try {
        VirtualFile root = contentEntry.getFile().getParent();
        VirtualFile sfRoot = root.createChildDirectory(this, "singleFile");
        VirtualFile aFile = sfRoot.createChildData(this, "A.java");
        aFile.setBinaryContent("package com.example;\npublic class A {}\n".getBytes(StandardCharsets.UTF_8));
        // bFile is not registered as a root
        VirtualFile bFile = sfRoot.createChildData(this, "B.java");
        bFile.setBinaryContent("package com.example;\npublic class B {}\n".getBytes(StandardCharsets.UTF_8));
        ContentEntry newEntry = model.addContentEntry(sfRoot);
        newEntry.addSourceFolder(aFile, false).setPackagePrefix("com.example");
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  };

  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return MY_DESCRIPTOR;
  }

  public void testSingleFileRootResolve() {
    myFixture.configureByText("B.java", """
      import com.example.*;
      
      class Use extends A {
        void test() {
          A a = new A();
          <error descr="Cannot resolve symbol 'B'">B</error> b = new <error descr="Cannot resolve symbol 'B'">B</error>();
        }
      }
      """);
    myFixture.checkHighlighting();
    PsiClass aClass = JavaPsiFacade.getInstance(getProject()).findClass("com.example.A", GlobalSearchScope.projectScope(getProject()));
    assertNotNull(aClass);
    // Works for single-file roots as well
    String pkg = PackageIndex.getInstance(getProject()).getPackageNameByDirectory(aClass.getContainingFile().getVirtualFile());
    assertEquals("com.example", pkg);
  }
}
