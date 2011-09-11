/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

public class DuplicationsData {

  private Resource resource;
  private Set<Integer> duplicatedLines = new HashSet<Integer>();
  private double duplicatedBlocks;
  private List<XmlEntry> duplicationXMLEntries = new ArrayList<XmlEntry>();

  private SensorContext context;

  public DuplicationsData(Resource resource, SensorContext context) {
    this.resource = resource;
    this.context = context;
  }

  public void cumulate(String targetResource, int targetDuplicationStartLine, int duplicationStartLine, int duplicatedLines) {
    duplicationXMLEntries.add(new XmlEntry(targetResource, targetDuplicationStartLine, duplicationStartLine, duplicatedLines));
    for (int duplicatedLine = duplicationStartLine; duplicatedLine < duplicationStartLine + duplicatedLines; duplicatedLine++) {
      this.duplicatedLines.add(duplicatedLine);
    }
  }

  public void cumulate(Resource targetResource, int targetDuplicationStartLine, int duplicationStartLine, int duplicatedLines) {
    cumulate(context.saveResource(targetResource), targetDuplicationStartLine, duplicationStartLine, duplicatedLines);
  }

  public void incrementDuplicatedBlock() {
    duplicatedBlocks++;
  }

  public void save() {
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

  private static final class XmlEntry {
    private String target;
    private int targetStartLine;
    private int startLine;
    private int lines;

    private XmlEntry(String target, int targetStartLine, int startLine, int lines) {
      this.target = target;
      this.targetStartLine = targetStartLine;
      this.startLine = startLine;
      this.lines = lines;
    }

    @Override
    public String toString() {
      return new StringBuilder().append("<duplication lines=\"").append(lines)
          .append("\" start=\"").append(startLine)
          .append("\" target-start=\"").append(targetStartLine)
          .append("\" target-resource=\"").append(target).append("\"/>")
          .toString();
    }
  }

}
