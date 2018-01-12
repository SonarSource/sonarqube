/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.scm;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.protobuf.DbFileSources;

/**
 * ScmInfo implementation based on the lines stored in DB
 */
@Immutable
class DbScmInfo implements ScmInfo {

  private final ScmInfo delegate;
  private final String fileHash;

  private DbScmInfo(ScmInfo delegate, String fileHash) {
    this.delegate = delegate;
    this.fileHash = fileHash;
  }

  public static Optional<DbScmInfo> create(Iterable<DbFileSources.Line> lines, String fileHash) {
    LineToChangeset lineToChangeset = new LineToChangeset();
    Map<Integer, Changeset> lineChanges = new LinkedHashMap<>();

    for (DbFileSources.Line line : lines) {
      Changeset changeset = lineToChangeset.apply(line);
      if (changeset == null) {
        continue;
      }
      lineChanges.put(line.getLine(), changeset);
    }
    if (lineChanges.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new DbScmInfo(new ScmInfoImpl(lineChanges), fileHash));
  }
  
  public String fileHash() {
    return fileHash;
  }

  @Override
  public Changeset getLatestChangeset() {
    return delegate.getLatestChangeset();
  }

  @Override
  public Changeset getChangesetForLine(int lineNumber) {
    return delegate.getChangesetForLine(lineNumber);
  }

  @Override
  public boolean hasChangesetForLine(int lineNumber) {
    return delegate.hasChangesetForLine(lineNumber);
  }

  @Override
  public Map<Integer, Changeset> getAllChangesets() {
    return delegate.getAllChangesets();
  }

  /**
   * Transforms {@link org.sonar.db.protobuf.DbFileSources.Line} into {@link Changeset} 
   */
  private static class LineToChangeset implements Function<DbFileSources.Line, Changeset> {
    private final Changeset.Builder builder = Changeset.newChangesetBuilder();
    private final HashMap<Changeset, Changeset> cache = new HashMap<>();

    @Override
    @Nullable
    public Changeset apply(@Nonnull DbFileSources.Line input) {
      if (input.hasScmDate()) {
        Changeset cs = builder
          .setRevision(input.hasScmRevision() ? input.getScmRevision() : null)
          .setAuthor(input.hasScmAuthor() ? input.getScmAuthor() : null)
          .setDate(input.getScmDate())
          .build();
        if (cache.containsKey(cs)) {
          return cache.get(cs);
        }
        cache.put(cs, cs);
        return cs;
      }

      return null;
    }
  }
}
