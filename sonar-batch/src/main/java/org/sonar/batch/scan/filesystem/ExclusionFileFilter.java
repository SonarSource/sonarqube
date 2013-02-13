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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;

public class ExclusionFileFilter implements FileFilter, ResourceFilter, BatchComponent {
  private final FilePattern[] sourceInclusions;
  private final FilePattern[] testInclusions;
  private final FilePattern[] sourceExclusions;
  private final FilePattern[] testExclusions;

  public ExclusionFileFilter(FileExclusions exclusions) {
    sourceInclusions = FilePattern.create(exclusions.sourceInclusions());
    log("Included sources: ", sourceInclusions);

    sourceExclusions = FilePattern.create(exclusions.sourceExclusions());
    log("Excluded sources: ", sourceExclusions);

    testInclusions = FilePattern.create(exclusions.testInclusions());
    log("Included tests: ", testInclusions);

    testExclusions = FilePattern.create(exclusions.testExclusions());
    log("Excluded tests: ", testExclusions);
  }

  private void log(String title, FilePattern[] patterns) {
    if (patterns.length > 0) {
      Logger log = LoggerFactory.getLogger(ExclusionFileFilter.class);
      log.info(title);
      for (FilePattern pattern : patterns) {
        log.info("  " + pattern);
      }
    }
  }

  public boolean accept(File file, Context context) {
    FilePattern[] inclusionPatterns = (context.fileType() == FileType.TEST ? testInclusions : sourceInclusions);
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (FilePattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(context);
      }
      if (!matchInclusion) {
        return false;
      }
    }
    FilePattern[] exclusionPatterns = (context.fileType() == FileType.TEST ? testExclusions : sourceExclusions);
    for (FilePattern pattern : exclusionPatterns) {
      if (pattern.match(context)) {
        return false;
      }
    }
    return true;
  }

  public boolean isIgnored(Resource resource) {
    if (ResourceUtils.isFile(resource)) {
      return isIgnoredFileResource(resource);
    }
    return false;
  }

  private boolean isIgnoredFileResource(Resource resource) {
    FilePattern[] inclusionPatterns = (ResourceUtils.isUnitTestClass(resource) ? testInclusions : sourceInclusions);
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (FilePattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(resource);
      }
      if (!matchInclusion) {
        return true;
      }
    }
    FilePattern[] exclusionPatterns = (ResourceUtils.isUnitTestClass(resource) ? testExclusions : sourceExclusions);
    for (FilePattern pattern : exclusionPatterns) {
      if (pattern.match(resource)) {
        return true;
      }
    }
    return false;
  }

  FilePattern[] sourceInclusions() {
    return sourceInclusions;
  }

  FilePattern[] testInclusions() {
    return testInclusions;
  }

  FilePattern[] sourceExclusions() {
    return sourceExclusions;
  }

  FilePattern[] testExclusions() {
    return testExclusions;
  }

  static abstract class FilePattern {
    final WildcardPattern pattern;

    protected FilePattern(String pattern) {
      this.pattern = WildcardPattern.create(pattern);
    }

    abstract boolean match(Context context);

    abstract boolean match(Resource resource);

    static FilePattern create(String s) {
      if (StringUtils.startsWithIgnoreCase(s, "file:")) {
        return new AbsolutePathPattern(StringUtils.substring(s, "file:".length()));
      }
      return new RelativePathPattern(s);
    }

    static FilePattern[] create(String[] s) {
      FilePattern[] result = new FilePattern[s.length];
      for (int i = 0; i < s.length; i++) {
        result[i] = FilePattern.create(s[i]);
      }
      return result;
    }
  }

  private static class AbsolutePathPattern extends FilePattern {
    private AbsolutePathPattern(String pattern) {
      super(pattern);
    }

    boolean match(Context context) {
      return pattern.match(context.fileCanonicalPath());
    }

    boolean match(Resource resource) {
      return false;
    }

    @Override
    public String toString() {
      return "file:" + pattern.toString();
    }
  }

  private static class RelativePathPattern extends FilePattern {
    private RelativePathPattern(String pattern) {
      super(pattern);
    }

    boolean match(Context context) {
      return pattern.match(context.fileRelativePath());
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
