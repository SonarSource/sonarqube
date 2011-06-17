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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public final class SourceChecksum {

  private static final String SPACE_CHARS = "\t\n\r ";

  private SourceChecksum() {
    // only static methods
  }

  public static List<String> lineChecksumsOfFile(String file) {
    List<String> result = Lists.newArrayList();
    if (file != null) {
      String[] lines = file.split("\r?\n|\r", -1);
      for (String line : lines) {
        result.add(lineChecksum(line));
      }
    }
    return result;
  }

  public static String lineChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, SPACE_CHARS, "");
    return DigestUtils.md5Hex(reducedLine);
  }

}
