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
package org.sonar.api.scan.filesystem;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * @since 3.5
 */
public class FileQuery {

  public static FileQuery on(FileType... types) {
    return new FileQuery(types);
  }

  public static FileQuery onSource() {
    return on(FileType.SOURCE);
  }

  public static FileQuery onTest() {
    return on(FileType.TEST);
  }

  private final Set<FileType> types;
  private final Set<String> languages = Sets.newLinkedHashSet();
  private final Set<String> inclusions = Sets.newLinkedHashSet();
  private final Set<String> exclusions = Sets.newLinkedHashSet();
  private final Collection<FileFilter> filters = Lists.newLinkedList();

  private FileQuery(FileType... types) {
    this.types = Sets.newHashSet(types);
  }

  public Collection<FileType> types() {
    return types;
  }

  public Collection<String> languages() {
    return languages;
  }

  public FileQuery onLanguage(String... languages) {
    this.languages.addAll(Arrays.asList(languages));
    return this;
  }

  public Collection<String> inclusions() {
    return inclusions;
  }

  public FileQuery withInclusions(String... inclusions) {
    this.inclusions.addAll(Arrays.asList(inclusions));
    return this;
  }

  public Collection<String> exclusions() {
    return exclusions;
  }

  public FileQuery withExclusions(String... exclusions) {
    this.exclusions.addAll(Arrays.asList(exclusions));
    return this;
  }

  public Collection<FileFilter> filters() {
    return filters;
  }

  public FileQuery withFilters(FileFilter... filters) {
    this.filters.addAll(Arrays.asList(filters));
    return this;
  }
}

