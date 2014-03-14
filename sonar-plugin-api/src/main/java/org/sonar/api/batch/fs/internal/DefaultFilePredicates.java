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
package org.sonar.api.batch.fs.internal;

import com.google.common.collect.Lists;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Factory of {@link org.sonar.api.batch.fs.FilePredicate}
 *
 * @since 4.2
 */
public class DefaultFilePredicates implements FilePredicates {
  /**
   * Client code should use {@link org.sonar.api.batch.fs.FileSystem#predicates()} to get an instance
   */
  DefaultFilePredicates() {
  }

  /**
   * Returns a predicate that always evaluates to true
   */
  public FilePredicate all() {
    return TruePredicate.TRUE;
  }

  /**
   * Returns a predicate that always evaluates to false
   */
  public FilePredicate none() {
    return FalsePredicate.FALSE;
  }

  /**
   * Warning - not efficient because absolute path is not indexed yet.
   */
  public FilePredicate hasAbsolutePath(String s) {
    return new AbsolutePathPredicate(s);
  }

  /**
   * TODO document that non-normalized path and Windows-style path are supported
   */
  public FilePredicate hasRelativePath(String s) {
    return new RelativePathPredicate(s);
  }

  public FilePredicate matchesPathPattern(String inclusionPattern) {
    return new PathPatternPredicate(PathPattern.create(inclusionPattern));
  }

  public FilePredicate matchesPathPatterns(String[] inclusionPatterns) {
    if (inclusionPatterns.length == 0) {
      return TruePredicate.TRUE;
    }
    FilePredicate[] predicates = new FilePredicate[inclusionPatterns.length];
    for (int i = 0; i < inclusionPatterns.length; i++) {
      predicates[i] = new PathPatternPredicate(PathPattern.create(inclusionPatterns[i]));
    }
    return or(predicates);
  }

  public FilePredicate doesNotMatchPathPattern(String exclusionPattern) {
    return not(matchesPathPattern(exclusionPattern));
  }

  public FilePredicate doesNotMatchPathPatterns(String[] exclusionPatterns) {
    if (exclusionPatterns.length == 0) {
      return TruePredicate.TRUE;
    }
    return not(matchesPathPatterns(exclusionPatterns));
  }

  public FilePredicate hasPath(String s) {
    File file = new File(s);
    if (file.isAbsolute()) {
      return hasAbsolutePath(s);
    }
    return hasRelativePath(s);
  }

  public FilePredicate is(File ioFile) {
    if (ioFile.isAbsolute()) {
      return hasAbsolutePath(ioFile.getAbsolutePath());
    }
    return hasRelativePath(ioFile.getPath());
  }

  public FilePredicate hasLanguage(String language) {
    return new LanguagePredicate(language);
  }

  public FilePredicate hasLanguages(Collection<String> languages) {
    List<FilePredicate> list = Lists.newArrayList();
    for (String language : languages) {
      list.add(hasLanguage(language));
    }
    return or(list);
  }

  public FilePredicate hasStatus(InputFile.Status status) {
    return new StatusPredicate(status);
  }

  public FilePredicate hasType(InputFile.Type type) {
    return new TypePredicate(type);
  }

  public FilePredicate not(FilePredicate p) {
    return new NotPredicate(p);
  }

  public FilePredicate or(Collection<FilePredicate> or) {
    return new OrPredicate(or);
  }

  public FilePredicate or(FilePredicate... or) {
    return new OrPredicate(Arrays.asList(or));
  }

  public FilePredicate or(FilePredicate first, FilePredicate second) {
    return new OrPredicate(Arrays.asList(first, second));
  }

  public FilePredicate and(Collection<FilePredicate> and) {
    return new AndPredicate(and);
  }

  public FilePredicate and(FilePredicate... and) {
    return new AndPredicate(Arrays.asList(and));
  }

  public FilePredicate and(FilePredicate first, FilePredicate second) {
    return new AndPredicate(Arrays.asList(first, second));
  }
}
