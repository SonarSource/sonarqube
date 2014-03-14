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

import java.io.File;
import java.util.Collection;

/**
 * Factory of {@link org.sonar.api.batch.fs.FilePredicate}
 *
 * @since 4.2
 */
public interface FilePredicates {
  /**
   * Returns a predicate that always evaluates to true
   */
  FilePredicate all();

  /**
   * Returns a predicate that always evaluates to false
   */
  FilePredicate none();

  /**
   * Warning - not efficient because absolute path is not indexed yet.
   */
  FilePredicate hasAbsolutePath(String s);

  /**
   * TODO document that non-normalized path and Windows-style path are supported
   */
  FilePredicate hasRelativePath(String s);

  FilePredicate matchesPathPattern(String inclusionPattern);

  FilePredicate matchesPathPatterns(String[] inclusionPatterns);

  FilePredicate doesNotMatchPathPattern(String exclusionPattern);

  FilePredicate doesNotMatchPathPatterns(String[] exclusionPatterns);

  FilePredicate hasPath(String s);

  FilePredicate is(File ioFile);

  FilePredicate hasLanguage(String language);

  FilePredicate hasLanguages(Collection<String> languages);

  FilePredicate hasStatus(InputFile.Status status);

  FilePredicate hasType(InputFile.Type type);

  FilePredicate not(FilePredicate p);

  FilePredicate or(Collection<FilePredicate> or);

  FilePredicate or(FilePredicate... or);

  FilePredicate or(FilePredicate first, FilePredicate second);

  FilePredicate and(Collection<FilePredicate> and);

  FilePredicate and(FilePredicate... and);

  FilePredicate and(FilePredicate first, FilePredicate second);

}
