/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.scan.filesystem.InputFile;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.WildcardPattern;

abstract class PathPattern {

  final WildcardPattern pattern;

  PathPattern(String pattern) {
    this.pattern = WildcardPattern.create(pattern);
  }

  abstract boolean match(Resource resource);

  abstract boolean match(InputFile inputFile);

  abstract boolean supportResource();

  static PathPattern create(String s) {
    String trimmed = StringUtils.trim(s);
    if (StringUtils.startsWithIgnoreCase(trimmed, "file:")) {
      return new AbsolutePathPattern(StringUtils.substring(trimmed, "file:".length()));
    }
    return new RelativePathPattern(trimmed);
  }

  static PathPattern[] create(String[] s) {
    PathPattern[] result = new PathPattern[s.length];
    for (int i = 0; i < s.length; i++) {
      result[i] = create(s[i]);
    }
    return result;
  }

  private static class AbsolutePathPattern extends PathPattern {
    private AbsolutePathPattern(String pattern) {
      super(pattern);
    }

    @Override
    boolean match(InputFile inputFile) {
      return pattern.match(inputFile.absolutePath());
    }

    @Override
    boolean match(Resource resource) {
      return false;
    }

    @Override
    boolean supportResource() {
      return false;
    }

    @Override
    public String toString() {
      return "file:" + pattern.toString();
    }
  }

  /**
   * Path relative to module basedir
   */
  private static class RelativePathPattern extends PathPattern {
    private RelativePathPattern(String pattern) {
      super(pattern);
    }

    @Override
    boolean match(InputFile inputFile) {
      String path = inputFile.path();
      return path != null && pattern.match(path);
    }

    @Override
    boolean match(Resource resource) {
      return resource.matchFilePattern(pattern.toString());
    }

    @Override
    boolean supportResource() {
      return true;
    }

    @Override
    public String toString() {
      return pattern.toString();
    }
  }
}
