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
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;

/**
 * @since 3.5
 */
class ExclusionFileFilter implements FileFilter {
  private final FileType fileType;
  private final WildcardPattern pattern;

  ExclusionFileFilter(FileType fileType, String pattern) {
    this.fileType = fileType;
    this.pattern = WildcardPattern.create(StringUtils.trim(pattern));
  }

  public boolean accept(File file, Context context) {
    return !fileType.equals(context.fileType()) || !pattern.match(context.fileRelativePath());
  }

  WildcardPattern pattern() {
    return pattern;
  }
}
