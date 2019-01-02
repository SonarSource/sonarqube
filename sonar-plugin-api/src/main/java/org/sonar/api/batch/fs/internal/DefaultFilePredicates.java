/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;

/**
 * Factory of {@link org.sonar.api.batch.fs.FilePredicate}
 *
 * @since 4.2
 */
public class DefaultFilePredicates implements FilePredicates {

  private final Path baseDir;

  /**
   * Client code should use {@link org.sonar.api.batch.fs.FileSystem#predicates()} to get an instance
   */
  public DefaultFilePredicates(Path baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * Returns a predicate that always evaluates to true
   */
  @Override
  public FilePredicate all() {
    return TruePredicate.TRUE;
  }

  /**
   * Returns a predicate that always evaluates to false
   */
  @Override
  public FilePredicate none() {
    return FalsePredicate.FALSE;
  }

  @Override
  public FilePredicate hasAbsolutePath(String s) {
    return new AbsolutePathPredicate(s, baseDir);
  }

  /**
   * non-normalized path and Windows-style path are supported
   */
  @Override
  public FilePredicate hasRelativePath(String s) {
    return new RelativePathPredicate(s);
  }

  @Override
  public FilePredicate hasFilename(String s) {
    return new FilenamePredicate(s);
  }

  @Override
  public FilePredicate hasExtension(String s) {
    return new FileExtensionPredicate(s);
  }

  @Override
  public FilePredicate hasURI(URI uri) {
    return new URIPredicate(uri, baseDir);
  }

  @Override
  public FilePredicate matchesPathPattern(String inclusionPattern) {
    return new PathPatternPredicate(PathPattern.create(inclusionPattern));
  }

  @Override
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

  @Override
  public FilePredicate doesNotMatchPathPattern(String exclusionPattern) {
    return not(matchesPathPattern(exclusionPattern));
  }

  @Override
  public FilePredicate doesNotMatchPathPatterns(String[] exclusionPatterns) {
    if (exclusionPatterns.length == 0) {
      return TruePredicate.TRUE;
    }
    return not(matchesPathPatterns(exclusionPatterns));
  }

  @Override
  public FilePredicate hasPath(String s) {
    File file = new File(s);
    if (file.isAbsolute()) {
      return hasAbsolutePath(s);
    }
    return hasRelativePath(s);
  }

  @Override
  public FilePredicate is(File ioFile) {
    if (ioFile.isAbsolute()) {
      return hasAbsolutePath(ioFile.getAbsolutePath());
    }
    return hasRelativePath(ioFile.getPath());
  }

  @Override
  public FilePredicate hasLanguage(String language) {
    return new LanguagePredicate(language);
  }

  @Override
  public FilePredicate hasLanguages(Collection<String> languages) {
    List<FilePredicate> list = new ArrayList<>();
    for (String language : languages) {
      list.add(hasLanguage(language));
    }
    return or(list);
  }

  @Override
  public FilePredicate hasLanguages(String... languages) {
    List<FilePredicate> list = new ArrayList<>();
    for (String language : languages) {
      list.add(hasLanguage(language));
    }
    return or(list);
  }

  @Override
  public FilePredicate hasType(InputFile.Type type) {
    return new TypePredicate(type);
  }

  @Override
  public FilePredicate not(FilePredicate p) {
    return new NotPredicate(p);
  }

  @Override
  public FilePredicate or(Collection<FilePredicate> or) {
    return OrPredicate.create(or);
  }

  @Override
  public FilePredicate or(FilePredicate... or) {
    return OrPredicate.create(Arrays.asList(or));
  }

  @Override
  public FilePredicate or(FilePredicate first, FilePredicate second) {
    return OrPredicate.create(Arrays.asList(first, second));
  }

  @Override
  public FilePredicate and(Collection<FilePredicate> and) {
    return AndPredicate.create(and);
  }

  @Override
  public FilePredicate and(FilePredicate... and) {
    return AndPredicate.create(Arrays.asList(and));
  }

  @Override
  public FilePredicate and(FilePredicate first, FilePredicate second) {
    return AndPredicate.create(Arrays.asList(first, second));
  }

  @Override
  public FilePredicate hasStatus(Status status) {
    return new StatusPredicate(status);
  }

  @Override
  public FilePredicate hasAnyStatus() {
    return new StatusPredicate(null);
  }
}
