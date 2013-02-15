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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.utils.WildcardPattern;

abstract class PathPattern {
  final WildcardPattern pattern;

  PathPattern(String pattern) {
    this.pattern = WildcardPattern.create(pattern);
  }

  abstract boolean match(FileSystemFilter.Context context);

  abstract boolean match(Resource resource);

  static PathPattern create(String s) {
    if (StringUtils.startsWithIgnoreCase(s, "file:")) {
      return new AbsolutePathPattern(StringUtils.substring(s, "file:".length()));
    }
    return new RelativePathPattern(s);
  }

  static PathPattern[] create(String[] s) {
    PathPattern[] result = new PathPattern[s.length];
    for (int i = 0; i < s.length; i++) {
      result[i] = PathPattern.create(s[i]);
    }
    return result;
  }

  private static class AbsolutePathPattern extends PathPattern {
    private AbsolutePathPattern(String pattern) {
      super(pattern);
    }

    boolean match(FileSystemFilter.Context context) {
      return pattern.match(context.canonicalPath());
    }

    boolean match(Resource resource) {
      return false;
    }

    @Override
    public String toString() {
      return "file:" + pattern.toString();
    }
  }

  private static class RelativePathPattern extends PathPattern {
    private RelativePathPattern(String pattern) {
      super(pattern);
    }

    boolean match(FileSystemFilter.Context context) {
      return pattern.match(context.relativePath());
    }

    boolean match(Resource resource) {
      return resource.matchFilePattern(pattern.toString());
    }

    @Override
    public String toString() {
      return pattern.toString();
    }
  }
}
