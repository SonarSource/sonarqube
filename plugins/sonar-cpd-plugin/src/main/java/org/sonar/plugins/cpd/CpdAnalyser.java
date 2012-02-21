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

import net.sourceforge.pmd.cpd.TokenEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.duplications.cpd.Match;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

        DuplicationsData firstFileData = getDuplicationsData(duplicationsData, firstFile);
        firstFileData.incrementDuplicatedBlock();

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

          String resourceKey = SonarEngine.getFullKey(project, secondFile);
          firstFileData.cumulate(resourceKey, secondLine, firstLine, match.getLineCount());
        }
      }
    }

    for (Map.Entry<Resource, DuplicationsData> entry : duplicationsData.entrySet()) {
      entry.getValue().save(context, entry.getKey());
    }
  }

  private DuplicationsData getDuplicationsData(Map<Resource, DuplicationsData> fileContainer, Resource file) {
    DuplicationsData data = fileContainer.get(file);
    if (data == null) {
      String resourceKey = SonarEngine.getFullKey(project, file);
      data = new DuplicationsData(resourceKey);
      fileContainer.put(file, data);
    }
    return data;
  }

}
