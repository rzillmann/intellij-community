// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.bookmark.ui.tree

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.tree.TreePath

internal class VirtualFileVisitor(val file: VirtualFile, val collector: MutableList<TreePath>?) : TreeVisitor {
  override fun visitThread(): TreeVisitor.VisitThread = TreeVisitor.VisitThread.BGT

  private fun found(path: TreePath) = collector?.add(path)?.let { TreeVisitor.Action.SKIP_CHILDREN } ?: TreeVisitor.Action.INTERRUPT

  override fun visit(path: TreePath): TreeVisitor.Action = when (val node = TreeUtil.getLastUserObject(path)) {
    is RootNode -> TreeVisitor.Action.CONTINUE
    is GroupNode -> TreeVisitor.Action.CONTINUE
    is ProjectViewNode<*> -> when {
      node.canRepresent(file) -> found(path)
      node.contains(file) -> TreeVisitor.Action.CONTINUE
      else -> TreeVisitor.Action.SKIP_CHILDREN
    }
    else -> TreeVisitor.Action.SKIP_CHILDREN
  }
}
