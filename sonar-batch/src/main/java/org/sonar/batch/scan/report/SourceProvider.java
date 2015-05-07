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
package org.sonar.batch.scan.report;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.scan.filesystem.InputPathCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BatchSide
public class SourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SourceProvider.class);
  private final InputPathCache inputPathCache;
  private final FileSystem fs;

  public SourceProvider(InputPathCache inputPathCache, FileSystem fs) {
    this.inputPathCache = inputPathCache;
    this.fs = fs;
  }

  public List<String> getEscapedSource(BatchResource component) {
    if (!component.isFile()) {
      // Folder
      return Collections.emptyList();
    }
    try {
      InputFile inputFile = (InputFile) inputPathCache.getInputPath(component);
      List<String> lines = FileUtils.readLines(inputFile.file(), fs.encoding());
      List<String> escapedLines = new ArrayList<String>(lines.size());
      for (String line : lines) {
        escapedLines.add(StringEscapeUtils.escapeHtml(line));
      }
      return escapedLines;
    } catch (IOException e) {
      LOG.warn("Unable to read source code of resource {}", component, e);
      return Collections.emptyList();
    }
  }
}
