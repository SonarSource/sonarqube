/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.pmd.cpd.TokenEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.duplications.cpd.Match;

public class CpdAnalyser {

  private static final Logger LOG = LoggerFactory.getLogger(CpdAnalyser.class);

  private CpdMapping mapping;
  private SensorContext context;
  private Project project;

  public CpdAnalyser(Project project, SensorContext context, CpdMapping mapping) {
    this.mapping = mapping;
    this.context = context;
    this.project = project;
  }

  public void analyse(Iterator<Match> matches) {
    Map<Resource, DuplicationsData> duplicationsData = new HashMap<Resource, DuplicationsData>();
    while (matches.hasNext()) {
      Match match = matches.next();

      for (TokenEntry firstMark : match.getMarkSet()) {
        String firstAbsolutePath = firstMark.getTokenSrcID();
        int firstLine = firstMark.getBeginLine();

        Resource firstFile = mapping.createResource(new File(firstAbsolutePath), project.getFileSystem().getSourceDirs());
        if (firstFile == null) {
          LOG.warn("CPD - File not found : {}", firstAbsolutePath);
          continue;
        }

        for (TokenEntry tokenEntry : match.getMarkSet()) {
          String secondAbsolutePath = tokenEntry.getTokenSrcID();
          int secondLine = tokenEntry.getBeginLine();
          if (secondAbsolutePath.equals(firstAbsolutePath) && firstLine == secondLine) {
            continue;
          }
          Resource secondFile = mapping.createResource(new File(secondAbsolutePath), project.getFileSystem().getSourceDirs());
          if (secondFile == null) {
            LOG.warn("CPD - File not found : {}", secondAbsolutePath);
            continue;
          }

          processClassMeasure(duplicationsData, firstFile, firstLine, secondFile, secondLine, match.getLineCount());
        }
      }
    }

    for (DuplicationsData data : duplicationsData.values()) {
      data.saveUsing(context);
    }
  }

  private void processClassMeasure(Map<Resource, DuplicationsData> fileContainer, Resource file, int duplicationStartLine,
      Resource targetFile, int targetDuplicationStartLine, int duplicatedLines) {
    if (file != null && targetFile != null) {
      DuplicationsData data = fileContainer.get(file);
      if (data == null) {
        data = new DuplicationsData(file, context);
        fileContainer.put(file, data);
      }
      data.cumulate(targetFile, targetDuplicationStartLine, duplicationStartLine, duplicatedLines);
    }
  }

  private static final class DuplicationsData {

    protected Set<Integer> duplicatedLines = new HashSet<Integer>();
    protected double duplicatedBlocks;
    protected Resource resource;
    private SensorContext context;
    private List<StringBuilder> duplicationXMLEntries = new ArrayList<StringBuilder>();

    private DuplicationsData(Resource resource, SensorContext context) {
      this.context = context;
      this.resource = resource;
    }

    protected void cumulate(Resource targetResource, int targetDuplicationStartLine, int duplicationStartLine, int duplicatedLines) {
      StringBuilder xml = new StringBuilder();
      xml.append("<duplication lines=\"").append(duplicatedLines).append("\" start=\"").append(duplicationStartLine).append(
          "\" target-start=\"").append(targetDuplicationStartLine).append("\" target-resource=\"").append(
          context.saveResource(targetResource)).append("\"/>");

      duplicationXMLEntries.add(xml);

      int duplicatedLinesBefore = this.duplicatedLines.size();
      for (int duplicatedLine = duplicationStartLine; duplicatedLine < duplicationStartLine + duplicatedLines; duplicatedLine++) {
        this.duplicatedLines.add(duplicatedLine);
      }
      this.duplicatedBlocks++;
    }

    protected void saveUsing(SensorContext context) {
      context.saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
      context.saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, (double) duplicatedLines.size());
      context.saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, duplicatedBlocks);
      context.saveMeasure(resource, new Measure(CoreMetrics.DUPLICATIONS_DATA, getDuplicationXMLData()));
    }

    private String getDuplicationXMLData() {
      StringBuilder duplicationXML = new StringBuilder("<duplications>");
      for (StringBuilder xmlEntry : duplicationXMLEntries) {
        duplicationXML.append(xmlEntry);
      }
      duplicationXML.append("</duplications>");
      return duplicationXML.toString();
    }
  }
}
