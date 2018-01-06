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
package org.sonar.server.computation.task.projectanalysis.source;

import com.google.common.base.Joiner;
import java.util.Iterator;
import java.util.List;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.hash.SourceLinesHashesComputer;
import org.sonar.db.protobuf.DbFileSources;

public class ComputeFileSourceData {

  private final List<LineReader> lineReaders;
  private final Iterator<String> linesIterator;

  private final int numberOfLines;
  private int currentLine;

  public ComputeFileSourceData(Iterator<String> sourceLinesIterator, List<LineReader> dataLineReaders, int numberOfLines) {
    this.lineReaders = dataLineReaders;
    this.linesIterator = sourceLinesIterator;
    this.numberOfLines = numberOfLines;
    this.currentLine = 0;
  }

  public Data compute() {
    Data data = new Data(numberOfLines);
    while (linesIterator.hasNext()) {
      currentLine++;
      read(data, linesIterator.next(), linesIterator.hasNext());
    }
    return data;
  }

  private void read(Data data, String source, boolean hasNextLine) {
    data.linesHashesComputer.addLine(source);
    data.sourceHashComputer.addLine(source, hasNextLine);

    DbFileSources.Line.Builder lineBuilder = data.fileSourceBuilder.addLinesBuilder()
      .setSource(source)
      .setLine(currentLine);
    for (LineReader lineReader : lineReaders) {
      lineReader.read(lineBuilder);
    }
  }

  public static class Data {
    private static final Joiner LINE_RETURN_JOINER = Joiner.on('\n');

    private final SourceLinesHashesComputer linesHashesComputer;
    private final SourceHashComputer sourceHashComputer = new SourceHashComputer();
    private final DbFileSources.Data.Builder fileSourceBuilder = DbFileSources.Data.newBuilder();

    public Data(int lineCount) {
      this.linesHashesComputer = new SourceLinesHashesComputer(lineCount);
    }

    public String getSrcHash() {
      return sourceHashComputer.getHash();
    }

    public String getLineHashes() {
      return LINE_RETURN_JOINER.join(linesHashesComputer.getLineHashes());
    }

    public DbFileSources.Data getFileSourceData() {
      return fileSourceBuilder.build();
    }
  }

}
