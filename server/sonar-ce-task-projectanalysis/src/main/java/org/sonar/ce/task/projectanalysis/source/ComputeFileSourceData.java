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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.base.Joiner;
import java.util.Iterator;
import java.util.List;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepositoryImpl.LineHashesComputer;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;

public class ComputeFileSourceData {
  private static final Joiner LINE_RETURN_JOINER = Joiner.on('\n');

  private final List<LineReader> lineReaders;
  private final Iterator<String> linesIterator;
  private final SourceHashComputer sourceHashComputer;
  private final LineHashesComputer lineHashesComputer;

  public ComputeFileSourceData(Iterator<String> sourceLinesIterator, List<LineReader> dataLineReaders, LineHashesComputer lineHashesComputer) {
    this.lineReaders = dataLineReaders;
    this.linesIterator = sourceLinesIterator;
    this.lineHashesComputer = lineHashesComputer;
    this.sourceHashComputer = new SourceHashComputer();
  }

  public Data compute() {
    DbFileSources.Data.Builder fileSourceBuilder = DbFileSources.Data.newBuilder();
    int currentLine = 0;

    while (linesIterator.hasNext()) {
      currentLine++;
      read(fileSourceBuilder, currentLine, linesIterator.next(), linesIterator.hasNext());
    }

    return new Data(fileSourceBuilder.build(), lineHashesComputer.getResult(), sourceHashComputer.getHash());
  }

  private void read(DbFileSources.Data.Builder fileSourceBuilder, int currentLine, String lineSource, boolean hasNextLine) {
    sourceHashComputer.addLine(lineSource, hasNextLine);
    lineHashesComputer.addLine(lineSource);

    DbFileSources.Line.Builder lineBuilder = fileSourceBuilder
      .addLinesBuilder()
      .setSource(lineSource)
      .setLine(currentLine);

    for (LineReader lineReader : lineReaders) {
      lineReader.read(lineBuilder);
    }
  }

  public static class Data {
    private final DbFileSources.Data fileSourceData;
    private final List<String> lineHashes;
    private final String srcHash;

    private Data(DbFileSources.Data fileSourceData, List<String> lineHashes, String srcHash) {
      this.fileSourceData = fileSourceData;
      this.lineHashes = lineHashes;
      this.srcHash = srcHash;
    }

    public String getSrcHash() {
      return srcHash;
    }

    public List<String> getLineHashes() {
      return lineHashes;
    }

    public DbFileSources.Data getFileSourceData() {
      return fileSourceData;
    }
  }

}
