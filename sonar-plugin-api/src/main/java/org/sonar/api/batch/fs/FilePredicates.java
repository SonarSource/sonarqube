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
package org.sonar.api.batch.fs;

import com.google.common.collect.Lists;
import org.sonar.api.batch.fs.internal.PathPattern;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Factory of {@link org.sonar.api.batch.fs.FilePredicate}
 *
 * @since 4.2
 */
public class FilePredicates {
  private static final FilePredicate ALWAYS_TRUE = new AlwaysTruePredicate();
  private static final FilePredicate ALWAYS_FALSE = not(ALWAYS_TRUE);

  private FilePredicates() {
    // only static stuff
  }

  /**
   * Returns a predicate that always evaluates to true
   */
  public static FilePredicate all() {
    return ALWAYS_TRUE;
  }

  /**
   * Returns a predicate that always evaluates to false
   */
  public static FilePredicate none() {
    return ALWAYS_FALSE;
  }

  /**
   * Warning - not efficient because absolute path is not indexed yet.
   */
  public static FilePredicate hasAbsolutePath(String s) {
    return new AbsolutePathPredicate(s);
  }

  /**
   * TODO document that non-normalized path and Windows-style path are supported
   */
  public static FilePredicate hasRelativePath(String s) {
    return new RelativePathPredicate(s);
  }

  public static FilePredicate matchesPathPattern(String inclusionPattern) {
    return new PathPatternPredicate(PathPattern.create(inclusionPattern));
  }

  public static FilePredicate matchesPathPatterns(String[] inclusionPatterns) {
    if (inclusionPatterns.length == 0) {
      return ALWAYS_TRUE;
    }
    FilePredicate[] predicates = new FilePredicate[inclusionPatterns.length];
    for (int i = 0; i < inclusionPatterns.length; i++) {
      predicates[i] = new PathPatternPredicate(PathPattern.create(inclusionPatterns[i]));
    }
    return or(predicates);
  }

  public static FilePredicate doesNotMatchPathPattern(String exclusionPattern) {
    return not(matchesPathPattern(exclusionPattern));
  }

  public static FilePredicate doesNotMatchPathPatterns(String[] exclusionPatterns) {
    if (exclusionPatterns.length == 0) {
      return ALWAYS_TRUE;
    }
    return not(matchesPathPatterns(exclusionPatterns));
  }

  public static FilePredicate hasPath(String s) {
    File file = new File(s);
    if (file.isAbsolute()) {
      return hasAbsolutePath(s);
    }
    return hasRelativePath(s);
  }

  public static FilePredicate is(File ioFile) {
    if (ioFile.isAbsolute()) {
      return hasAbsolutePath(ioFile.getAbsolutePath());
    }
    return hasRelativePath(ioFile.getPath());
  }

  public static FilePredicate hasLanguage(String language) {
    return new LanguagePredicate(language);
  }

  public static FilePredicate hasLanguages(Collection<String> languages) {
    List<FilePredicate> list = Lists.newArrayList();
    for (String language : languages) {
      list.add(hasLanguage(language));
    }
    return or(list);
  }

  public static FilePredicate hasStatus(InputFile.Status status) {
    return new StatusPredicate(status);
  }

  public static FilePredicate hasType(InputFile.Type type) {
    return new TypePredicate(type);
  }

  public static FilePredicate not(FilePredicate p) {
    return new NotPredicate(p);
  }

  public static FilePredicate or(Collection<FilePredicate> or) {
    return new OrPredicate(or);
  }

  public static FilePredicate or(FilePredicate... or) {
    return new OrPredicate(Arrays.asList(or));
  }

  public static FilePredicate or(FilePredicate first, FilePredicate second) {
    return new OrPredicate(Arrays.asList(first, second));
  }

  public static FilePredicate and(Collection<FilePredicate> and) {
    return new AndPredicate(and);
  }

  public static FilePredicate and(FilePredicate... and) {
    return new AndPredicate(Arrays.asList(and));
  }

  public static FilePredicate and(FilePredicate first, FilePredicate second) {
    return new AndPredicate(Arrays.asList(first, second));
  }

  private static class AlwaysTruePredicate implements FilePredicate {
    @Override
    public boolean apply(InputFile inputFile) {
      return true;
    }
  }
}
