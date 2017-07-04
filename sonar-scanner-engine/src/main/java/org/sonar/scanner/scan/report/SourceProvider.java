/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;

@ScannerSide
public class SourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SourceProvider.class);

  public List<String> getEscapedSource(DefaultInputComponent component) {
    if (!component.isFile()) {
      // Folder
      return Collections.emptyList();
    }
    try {
      InputFile inputFile = (InputFile) component;
      List<String> lines = FileUtils.readLines(inputFile.file(), inputFile.charset());
      List<String> escapedLines = new ArrayList<>(lines.size());
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
