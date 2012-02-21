/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cpd;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DuplicationsData {

  private final String resourceKey;
  private final Set<Integer> duplicatedLines = Sets.newHashSet();
  private final List<XmlEntry> duplicationXMLEntries = Lists.newArrayList();

  private double duplicatedBlocks;

  public DuplicationsData(String resourceKey) {
    this.resourceKey = resourceKey;
  }

  public void cumulate(String targetResourceKey, int targetDuplicationStartLine, int duplicationStartLine, int duplicatedLines) {
    duplicationXMLEntries.add(new XmlEntry(targetResourceKey, targetDuplicationStartLine, duplicationStartLine, duplicatedLines));
    for (int duplicatedLine = duplicationStartLine; duplicatedLine < duplicationStartLine + duplicatedLines; duplicatedLine++) {
      this.duplicatedLines.add(duplicatedLine);
    }
  }

  public void incrementDuplicatedBlock() {
    duplicatedBlocks++;
  }

  public void save(SensorContext context, Resource resource) {
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, (double) duplicatedLines.size());
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, duplicatedBlocks);
    context.saveMeasure(resource, new Measure(CoreMetrics.DUPLICATIONS_DATA, getDuplicationXMLData()));
  }

  private String getDuplicationXMLData() {
    Collections.sort(duplicationXMLEntries, COMPARATOR);
    StringBuilder duplicationXML = new StringBuilder("<duplications>");
    for (XmlEntry xmlEntry : duplicationXMLEntries) {
      duplicationXML.append(xmlEntry.toString());
    }
    duplicationXML.append("</duplications>");
    return duplicationXML.toString();
  }

  private static final Comparator<XmlEntry> COMPARATOR = new Comparator<XmlEntry>() {
    public int compare(XmlEntry o1, XmlEntry o2) {
      if (o1.startLine == o2.startLine) {
        return o1.lines - o2.lines;
      }
      return o1.startLine - o2.startLine;
    }
  };

  private final class XmlEntry {
    private final String target;
    private final int targetStartLine;
    private final int startLine;
    private final int lines;

    private XmlEntry(String target, int targetStartLine, int startLine, int lines) {
      this.target = target;
      this.targetStartLine = targetStartLine;
      this.startLine = startLine;
      this.lines = lines;
    }

    @Override
    public String toString() {
      return new StringBuilder()
          .append("<g>")
          .append("<b s=\"").append(startLine).append("\" l=\"").append(lines).append("\" r=\"").append(resourceKey).append("\" />")
          .append("<b s=\"").append(targetStartLine).append("\" l=\"").append(lines).append("\" r=\"").append(target).append("\" />")
          .append("</g>")
          .toString();
    }
  }

}
