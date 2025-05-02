// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.bazel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dependency.GraphDataOutput;
import org.jetbrains.jps.dependency.NodeSource;
import org.jetbrains.jps.javac.Iterators;

import java.io.IOException;
import java.util.List;

public interface SourceSnapshot {
  SourceSnapshot EMPTY = new SourceSnapshot() {
    @Override
    public @NotNull Iterable<@NotNull NodeSource> getSources() {
      return List.of();
    }

    @Override
    public @NotNull String getDigest(NodeSource src) {
      return "";
    }
  };

  @NotNull
  Iterable<@NotNull NodeSource> getSources();

  @NotNull
  String getDigest(NodeSource src);

  default void write(GraphDataOutput out) throws IOException {
    Iterable<@NotNull NodeSource> sources = getSources();
    out.writeInt(Iterators.count(sources));
    for (NodeSource src : sources) {
      out.writeUTF(getDigest(src));
      src.write(out);
    }
  }
}
