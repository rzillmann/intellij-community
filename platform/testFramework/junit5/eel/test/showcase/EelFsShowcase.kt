// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.testFramework.junit5.eel.showcase

import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.platform.eel.path.EelPath
import com.intellij.platform.eel.provider.utils.EelPathUtils
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.platform.testFramework.junit5.eel.fixture.IsolatedFileSystem
import com.intellij.platform.testFramework.junit5.eel.fixture.eelFixture
import com.intellij.testFramework.junit5.fixture.TestFixture
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestApplication
@Disabled("Depends on IJPL-160621")
class EelFsShowcase {

  val fsAndEelUnix: TestFixture<IsolatedFileSystem> = eelFixture(EelPath.OS.UNIX)

  val fsAndEelWindows: TestFixture<IsolatedFileSystem> = eelFixture(EelPath.OS.WINDOWS)

  fun EelPath.OS.osDependentFixture(): TestFixture<IsolatedFileSystem> = when (this) {
    EelPath.OS.WINDOWS -> fsAndEelWindows
    EelPath.OS.UNIX -> fsAndEelUnix
  }

  @Test
  fun `local path can be mapped back to eel paths`() {
    val fsdata = fsAndEelUnix.get()
    val localPath = fsdata.root.resolve("a").resolve("b").resolve("c")
    val mapper = fsdata.eelApi.mapper
    val eelPath = mapper.getOriginalPath(localPath)!!
    Assertions.assertEquals("/a/b/c", eelPath.toString())
  }

  @Test
  fun `windows api produces windows paths`() {
    val fsdata = fsAndEelWindows.get()
    val root = fsdata.fileSystem.rootDirectories.first()
    val mapper = fsdata.eelApi.mapper
    val eelPath = mapper.getOriginalPath(root)!!
    Assertions.assertTrue(OSAgnosticPathUtil.isAbsoluteDosPath(eelPath.toString()))
  }

  @Test
  fun `path mappings are invertible`() {
    val fsdata = fsAndEelUnix.get()
    val mapper = fsdata.eelApi.mapper
    val nio = fsdata.fileSystem.getPath("/a/b/c/d/e")
    val eel = EelPath.parse("/a/b/c/d/e", null)
    val eelNio = mapper.getOriginalPath(nio)!!
    val nioEel = mapper.toNioPath(eel)
    val nioEelNio = mapper.toNioPath(eelNio)
    val eelNioEel = mapper.getOriginalPath(nioEel)
    Assertions.assertEquals(nio, nioEel)
    Assertions.assertEquals(nioEel, nioEelNio)
    Assertions.assertEquals(eel, eelNio)
    Assertions.assertEquals(eelNio, eelNioEel)
  }

  @ParameterizedTest
  @EnumSource(EelPath.OS::class)
  fun `uri validity`(os: EelPath.OS) {
    val fs = os.osDependentFixture().get().fileSystem
    val root = fs.rootDirectories.first()
    val path = root.resolve("a/b/c/d/e")
    val uri = EelPathUtils.getUriLocalToEel(path)
    Assertions.assertEquals("file", uri.scheme)
    Assertions.assertNull(uri.authority)
    Assertions.assertTrue(uri.path.contains("a/b/c/d/e"))
  }
}