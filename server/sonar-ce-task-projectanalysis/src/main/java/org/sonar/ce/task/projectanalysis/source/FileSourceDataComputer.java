/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbFileSources;

public class FileSourceDataComputer {
  private final SourceLinesRepository sourceLinesRepository;
  private final SourceLineReadersFactory sourceLineReadersFactory;
  private final SourceLinesHashRepository sourceLinesHash;
  private final SourceHashComputer sourceHashComputer;

  public FileSourceDataComputer(SourceLinesRepository sourceLinesRepository, SourceLineReadersFactory sourceLineReadersFactory,
    SourceLinesHashRepository sourceLinesHash) {
    this.sourceLinesRepository = sourceLinesRepository;
    this.sourceLineReadersFactory = sourceLineReadersFactory;
    this.sourceLinesHash = sourceLinesHash;
    this.sourceHashComputer = new SourceHashComputer();
  }

  public Data compute(Component file, FileSourceDataWarnings fileSourceDataWarnings) {
    try (CloseableIterator<String> linesIterator = sourceLinesRepository.readLines(file);
      SourceLineReadersFactory.LineReaders lineReaders = sourceLineReadersFactory.getLineReaders(file)) {
      SourceLinesHashRepositoryImpl.LineHashesComputer lineHashesComputer = sourceLinesHash.getLineHashesComputerToPersist(file);
      DbFileSources.Data.Builder fileSourceBuilder = DbFileSources.Data.newBuilder();
      int currentLine = 0;

      while (linesIterator.hasNext()) {
        currentLine++;
        String lineSource = linesIterator.next();
        boolean hasNextLine = linesIterator.hasNext();

        sourceHashComputer.addLine(lineSource, hasNextLine);
        lineHashesComputer.addLine(lineSource);

        DbFileSources.Line.Builder lineBuilder = fileSourceBuilder
          .addLinesBuilder()
          .setSource(lineSource)
          .setLine(currentLine);
        lineReaders.read(lineBuilder, readError -> fileSourceDataWarnings.addWarning(file, readError));
      }

      Changeset latestChangeWithRevision = lineReaders.getLatestChangeWithRevision();
      return new Data(fileSourceBuilder.build(), lineHashesComputer.getResult(), sourceHashComputer.getHash(), latestChangeWithRevision);
    }
  }

  public static class Data {
    private final DbFileSources.Data fileSourceData;
    private final List<String> lineHashes;
    private final String srcHash;
    private final Changeset latestChangeWithRevision;

    public Data(DbFileSources.Data fileSourceData, List<String> lineHashes, String srcHash, @Nullable Changeset latestChangeWithRevision) {
      this.fileSourceData = fileSourceData;
      this.lineHashes = lineHashes;
      this.srcHash = srcHash;
      this.latestChangeWithRevision = latestChangeWithRevision;
    }

    public String getSrcHash() {
      return srcHash;
    }

    public List<String> getLineHashes() {
      return lineHashes;
    }

    public DbFileSources.Data getLineData() {
      return fileSourceData;
    }

    @CheckForNull
    public Changeset getLatestChangeWithRevision() {
      return latestChangeWithRevision;
    }
  }

}
