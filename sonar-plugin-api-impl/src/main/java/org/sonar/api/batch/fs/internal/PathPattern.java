/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.batch.fs.internal;

import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.WildcardPattern;

@ThreadSafe
public abstract class PathPattern {

  private static final Logger LOG = LoggerFactory.getLogger(PathPattern.class);

  /**
   * @deprecated since 6.6
   */
  @Deprecated
  private static final String ABSOLUTE_PATH_PATTERN_PREFIX = "file:";
  final WildcardPattern pattern;

  PathPattern(String pattern) {
    this.pattern = WildcardPattern.create(pattern);
  }

  public abstract boolean match(Path absolutePath, Path relativePath);

  public abstract boolean match(Path absolutePath, Path relativePath, boolean caseSensitiveFileExtension);

  public static PathPattern create(String s) {
    String trimmed = StringUtils.trim(s);
    if (StringUtils.startsWithIgnoreCase(trimmed, ABSOLUTE_PATH_PATTERN_PREFIX)) {
      LOG.warn("Using absolute path pattern is deprecated. Please use relative path instead of '" + trimmed + "'");
      return new AbsolutePathPattern(StringUtils.substring(trimmed, ABSOLUTE_PATH_PATTERN_PREFIX.length()));
    }
    return new RelativePathPattern(trimmed);
  }

  public static PathPattern[] create(String[] s) {
    return Arrays.stream(s).map(PathPattern::create).toArray(PathPattern[]::new);
  }

  /**
   * @deprecated since 6.6
   */
  @Deprecated
  private static class AbsolutePathPattern extends PathPattern {
    private AbsolutePathPattern(String pattern) {
      super(pattern);
    }

    @Override
    public boolean match(Path absolutePath, Path relativePath) {
      return match(absolutePath, relativePath, true);
    }

    @Override
    public boolean match(Path absolutePath, Path relativePath, boolean caseSensitiveFileExtension) {
      String path = PathUtils.sanitize(absolutePath.toString());
      if (!caseSensitiveFileExtension) {
        String extension = sanitizeExtension(FilenameUtils.getExtension(path));
        if (StringUtils.isNotBlank(extension)) {
          path = StringUtils.removeEndIgnoreCase(path, extension);
          path = path + extension;
        }
      }
      return pattern.match(path);
    }

    @Override
    public String toString() {
      return ABSOLUTE_PATH_PATTERN_PREFIX + pattern.toString();
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
    public boolean match(Path absolutePath, Path relativePath) {
      return match(absolutePath, relativePath, true);
    }

    @Override
    public boolean match(Path absolutePath, Path relativePath, boolean caseSensitiveFileExtension) {
      String path = PathUtils.sanitize(relativePath.toString());
      if (!caseSensitiveFileExtension) {
        String extension = sanitizeExtension(FilenameUtils.getExtension(path));
        if (StringUtils.isNotBlank(extension)) {
          path = StringUtils.removeEndIgnoreCase(path, extension);
          path = path + extension;
        }
      }
      return path != null && pattern.match(path);
    }

    @Override
    public String toString() {
      return pattern.toString();
    }
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
