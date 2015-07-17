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
package org.sonar.db.version.v50;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.CsvWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

class FileSourceDto {

  private static final String SPACE_CHARS = "\t\n\r ";

  private Iterator<String> sourceSplitter;

  private final Map<Integer, String> revisions;
  private final Map<Integer, String> authors;
  private final Map<Integer, String> dates;
  private final Map<Integer, String> utHits;
  private final Map<Integer, String> utConditions;
  private final Map<Integer, String> utCoveredConditions;
  private final Map<Integer, String> itHits;
  private final Map<Integer, String> itConditions;
  private final Map<Integer, String> itCoveredConditions;
  private final Map<Integer, String> overallHits;
  private final Map<Integer, String> overallConditions;
  private final Map<Integer, String> overallCoveredConditions;
  private final List<List<Block>> duplicationGroups;

  FileSourceDto(String source, String revisions, String authors, String dates,
    String utHits, String utConditions, String utCoveredConditions,
    String itHits, String itConditions, String itCoveredConditions,
    String overallHits, String overallConditions, String overallCoveredConditions, String duplicationData) {
    sourceSplitter = Splitter.onPattern("\r?\n|\r").split(source).iterator();
    this.revisions = KeyValueFormat.parseIntString(revisions);
    this.authors = KeyValueFormat.parseIntString(authors);
    this.dates = KeyValueFormat.parseIntString(dates);
    this.utHits = KeyValueFormat.parseIntString(utHits);
    this.utConditions = KeyValueFormat.parseIntString(utConditions);
    this.utCoveredConditions = KeyValueFormat.parseIntString(utCoveredConditions);
    this.itHits = KeyValueFormat.parseIntString(itHits);
    this.itConditions = KeyValueFormat.parseIntString(itConditions);
    this.itCoveredConditions = KeyValueFormat.parseIntString(itCoveredConditions);
    this.overallHits = KeyValueFormat.parseIntString(overallHits);
    this.overallConditions = KeyValueFormat.parseIntString(overallConditions);
    this.overallCoveredConditions = KeyValueFormat.parseIntString(overallCoveredConditions);
    this.duplicationGroups = StringUtils.isNotBlank(duplicationData) ? parseDuplicationData(duplicationData) : Collections.<List<Block>>emptyList();
  }

  String[] getSourceData() {
    String highlighting = "";
    String symbolRefs = "";
    Map<Integer, String> duplicationsPerLine = computeDuplicationsPerLine(duplicationGroups);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    int line = 0;
    String sourceLine = null;
    CsvWriter csv = CsvWriter.of(new OutputStreamWriter(output, UTF_8));
    StringBuilder lineHashes = new StringBuilder();
    while (sourceSplitter.hasNext()) {
      line++;
      sourceLine = sourceSplitter.next();
      lineHashes.append(lineChecksum(sourceLine)).append("\n");
      csv.values(revisions.get(line), authors.get(line), dates.get(line),
        utHits.get(line), utConditions.get(line), utCoveredConditions.get(line),
        itHits.get(line), itConditions.get(line), itCoveredConditions.get(line),
        overallHits.get(line), overallConditions.get(line), overallCoveredConditions.get(line),
        highlighting, symbolRefs, duplicationsPerLine.get(line), sourceLine);
    }
    csv.close();
    return new String[] {new String(output.toByteArray(), UTF_8), lineHashes.toString()};
  }

  public static String lineChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, SPACE_CHARS, "");
    if (reducedLine.isEmpty()) {
      return "";
    }
    return DigestUtils.md5Hex(reducedLine);
  }

  private Map<Integer, String> computeDuplicationsPerLine(List<List<Block>> duplicationGroups) {
    Map<Integer, String> result = new HashMap<>();
    if (duplicationGroups.isEmpty()) {
      return result;
    }
    Map<Integer, StringBuilder> dupPerLine = new HashMap<>();
    int blockId = 1;
    for (List<Block> group : duplicationGroups) {
      Block originBlock = group.get(0);
      addBlock(blockId, originBlock, dupPerLine);
      blockId++;
      for (int i = 1; i < group.size(); i++) {
        Block duplicate = group.get(i);
        if (duplicate.resourceKey.equals(originBlock.resourceKey)) {
          addBlock(blockId, duplicate, dupPerLine);
          blockId++;
        }
      }
    }
    for (Map.Entry<Integer, StringBuilder> entry : dupPerLine.entrySet()) {
      result.put(entry.getKey(), entry.getValue().toString());
    }
    return result;
  }

  private void addBlock(int blockId, Block block, Map<Integer, StringBuilder> dupPerLine) {
    int currentLine = block.start;
    for (int i = 0; i < block.length; i++) {
      if (dupPerLine.get(currentLine) == null) {
        dupPerLine.put(currentLine, new StringBuilder());
      }
      if (dupPerLine.get(currentLine).length() > 0) {
        dupPerLine.get(currentLine).append(',');
      }
      dupPerLine.get(currentLine).append(blockId);
      currentLine++;
    }

  }

  /**
   * Parses data of {@link CoreMetrics#DUPLICATIONS_DATA}.
   */
  private static List<List<Block>> parseDuplicationData(String data) {
    ImmutableList.Builder<List<Block>> groups = ImmutableList.builder();
    try {
      StringReader reader = new StringReader(data);
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      // <duplications>
      rootC.advance();
      SMInputCursor groupsCursor = rootC.childElementCursor("g");
      while (groupsCursor.getNext() != null) {
        // <g>
        SMInputCursor blocksCursor = groupsCursor.childElementCursor("b");
        ImmutableList.Builder<Block> group = ImmutableList.builder();
        while (blocksCursor.getNext() != null) {
          // <b>
          String resourceKey = blocksCursor.getAttrValue("r");
          int firstLine = getAttrIntValue(blocksCursor, "s");
          int numberOfLines = getAttrIntValue(blocksCursor, "l");

          group.add(new Block(resourceKey, firstLine, numberOfLines));
        }
        groups.add(group.build());
      }
    } catch (Exception e) {
      // SONAR-6174 Ignore any issue while parsing duplication measure. There is nothing user can do and things will get solved after
      // next analysis anyway
    }
    return groups.build();
  }

  private static int getAttrIntValue(SMInputCursor cursor, String attrName) throws XMLStreamException {
    return cursor.getAttrIntValue(cursor.findAttrIndex(null, attrName));
  }

  private static SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    return new SMInputFactory(xmlFactory);
  }

  private static class Block {

    final String resourceKey;
    final int start;
    final int length;

    public Block(String resourceKey, int s, int l) {
      this.resourceKey = resourceKey;
      this.start = s;
      this.length = l;
    }
  }

}
