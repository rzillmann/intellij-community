// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.gradle

import com.intellij.codeInsight.EditorInfo
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.runInEdtAndWait
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinTestUtils
import org.jetbrains.kotlin.idea.test.TagsTestDataUtil
import org.jetbrains.kotlin.idea.test.TagsTestDataUtil.TagInfo
import org.junit.Assert
import java.io.File

abstract class GradleDaemonAnalyzerTestCase(
    val testLineMarkers: Boolean,
    val checkWarnings: Boolean,
    val checkInfos: Boolean,
    private val rootDisposable: Disposable,
    private val sanitizer: (String) -> String = { it }
) : DaemonAnalyzerTestCase() {
    override fun doTestLineMarkers() = testLineMarkers

    fun checkHighlighting(project: Project, editor: Editor) {
        myProject = project
        try {
            runInEdtAndWait {
                // This will prepare ExpectedHighlightingData, clear current editor from testing tags, and then
                // call doCheckResult, which we override
                checkHighlighting(editor, checkWarnings, checkInfos)
            }
        } finally {
            myProject = null
        }
    }

    // We have to override doCheckResult because one from DaemonAnalyzerTestCase has few flaws:
    // - overlapping line markers lead to crash in 193 (fixed in later versions)
    // - it checks line markers and rest of highlighting *separately*, which means that we can't put both in
    //   expected test data
    // - no API to sanitize error descriptions (in particular, we have to remove endlines in diagnostic messages,
    //   otherwise parser in testdata goes completely insane)
    override fun doCheckResult(data: ExpectedHighlightingData, infos: Collection<HighlightInfo>, text: String) {
        performGenericHighlightingAndLineMarkersChecks(infos, text)
        performAdditionalChecksAfterHighlighting(editor)
    }

    private fun performGenericHighlightingAndLineMarkersChecks(infos: Collection<HighlightInfo>, text: String) {
        val lineMarkersTags: List<TagInfo<*>> = if (testLineMarkers) {
            TagsTestDataUtil.toLineMarkerTagPoints(
                DaemonCodeAnalyzerImpl.getLineMarkers(getDocument(file), project),
                /* withDescription = */ true
            )
        } else {
            emptyList()
        }

        val filteredHighlights = infos.filterNot {
            (it.severity == HighlightSeverity.INFORMATION || it.severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY) && !checkInfos ||
             it.severity == HighlightSeverity.WARNING && !checkWarnings
        }
        val highlightsTags: List<TagInfo<*>> = TagsTestDataUtil.toHighlightTagPoints(filteredHighlights)

        val allTags = lineMarkersTags + highlightsTags
        val actualTextWithTags = TagsTestDataUtil.insertTagsInText(allTags, text) { renderAdditionalAttributeForTag(it) }

        val physicalFileWithExpectedTestData = file.testDataFileByUserData
        KotlinTestUtils.assertEqualsToFile(physicalFileWithExpectedTestData, actualTextWithTags, sanitizer)
    }

    protected open fun performAdditionalChecksAfterHighlighting(editor: Editor) {}

    protected open fun renderAdditionalAttributeForTag(tag: TagInfo<*>): String? = null

    override fun getTestRootDisposable() = rootDisposable
}

fun checkFiles(
    files: List<VirtualFile>,
    project: Project,
    analyzer: GradleDaemonAnalyzerTestCase
) {
    var atLeastOneFile = false

    // We have to first load all files into the project and only then start
    // highlighting, otherwise cross-file references might not work
    val editors = files.map { file ->
        atLeastOneFile = true

        val originalText = VfsUtil.loadText(file)
        val textWithoutTags = textWithoutTags(originalText)

        configureEditorByExistingFile(file, project, textWithoutTags)
    }

    try {
        editors.forEach {
            analyzer.checkHighlighting(project, it)
        }
    } finally {
        files.forEach {
            runInEdtAndWait {
                FileEditorManager.getInstance(project).closeFile(it)
            }
        }
    }

    Assert.assertTrue(atLeastOneFile)
}

@IntellijInternalApi
fun textWithoutTags(text: String): String {
    val regex = "</?(error|warning|lineMarker).*?>".toRegex()
    return regex.replace(text, "")
}

private fun createEditor(file: VirtualFile, project: Project): Editor {
    val instance = FileEditorManager.getInstance(project)
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val editor = instance.openTextEditor(OpenFileDescriptor(project, file, 0), false)
    (editor as EditorImpl).setCaretActive()
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    DaemonCodeAnalyzerEx.getInstanceEx(project).restart("GradleDaemonAnalyzerTestCaseKt.createEditor");
    return editor
}

internal fun configureEditorByExistingFile(
    virtualFile: VirtualFile,
    project: Project,
    contentToSet: String
): Editor {
    var result: Editor? = null
    runInEdtAndWait {
        val editor = createEditor(virtualFile, project)
        val document = editor.document
        val editorInfo = EditorInfo(document.text)
        ApplicationManager.getApplication().runWriteAction {
            if (document.text != contentToSet) {
                document.setText(contentToSet)
            }

            editorInfo.applyToEditor(editor)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        result = editor
    }
    return result!!
}

/**
 * Returns original physical file with expected test data, i.e. something like 'kotlin/idea/testData/gradle/runConfigurations/myTest/Foo.kt'
 *
 * Note that it's different from physical file used in project during test run (which is usually a copy in temporary folder)
 *
 * All inheritors of GradleImportingTestCase should normally have PsiFiles with properly attached UserData to them
 * (see [org.jetbrains.kotlin.idea.codeInsight.gradle.KotlinGradleImportingTestCase.configureByFiles])
 */
internal val VirtualFile.testDataFileByUserData: File
    get() {
        val physicalFileWithExpectedTestData = File(this.getUserData(VfsTestUtil.TEST_DATA_FILE_PATH)!!)
        TestCase.assertTrue(
            "Can't find file with expected test data by absolute path ${physicalFileWithExpectedTestData.absolutePath}",
            physicalFileWithExpectedTestData.exists()
        )
        return physicalFileWithExpectedTestData
    }

internal val PsiFile.testDataFileByUserData: File
    get() = virtualFile.testDataFileByUserData
