/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.source;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.source.db.FileSourceDb;

import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;

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
    Data data = new Data();
    while (linesIterator.hasNext()) {
      currentLine++;
      read(data, linesIterator.next(), hasNextLine());
    }
    // Process last line
    if (hasNextLine()) {
      currentLine++;
      read(data, "", false);
    }
    return data;
  }

  private void read(Data data, String source, boolean hasNextLine) {
    if (hasNextLine) {
      data.lineHashes.append(computeLineChecksum(source)).append("\n");
      data.srcMd5Digest.update((source + "\n").getBytes(UTF_8));
    } else {
      data.lineHashes.append(computeLineChecksum(source));
      data.srcMd5Digest.update(source.getBytes(UTF_8));
    }

    FileSourceDb.Line.Builder lineBuilder = data.fileSourceBuilder.addLinesBuilder()
      .setSource(source)
      .setLine(currentLine);
    for (LineReader lineReader : lineReaders) {
      lineReader.read(lineBuilder);
    }
  }

  private static String computeLineChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, "\t ", "");
    if (reducedLine.isEmpty()) {
      return "";
    }
    return DigestUtils.md5Hex(reducedLine);
  }

  private boolean hasNextLine(){
    return linesIterator.hasNext() || currentLine < numberOfLines;
  }

  public static class Data {
    private final StringBuilder lineHashes;
    private final MessageDigest srcMd5Digest;
    private final FileSourceDb.Data.Builder fileSourceBuilder;

    public Data() {
      this.fileSourceBuilder = FileSourceDb.Data.newBuilder();
      this.lineHashes = new StringBuilder();
      this.srcMd5Digest = DigestUtils.getMd5Digest();
    }

    public String getSrcHash() {
      return Hex.encodeHexString(srcMd5Digest.digest());
    }

    public String getLineHashes() {
      return lineHashes.toString();
    }

    public FileSourceDb.Data getFileSourceData() {
      return fileSourceBuilder.build();
    }
  }
}
