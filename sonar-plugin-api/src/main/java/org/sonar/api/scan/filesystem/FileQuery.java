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
package org.sonar.api.scan.filesystem;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.EqualsBuilder;

import javax.annotation.Nullable;

import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @since 3.5
 * @deprecated in 4.2. Replaced by {@link org.sonar.api.batch.fs.FileSystem} and
 * {@link org.sonar.api.batch.fs.FilePredicate}
 */
@Deprecated
public class FileQuery {

  private final ListMultimap<String, String> attributes = ArrayListMultimap.create();
  private final Set<String> inclusions = Sets.newHashSet();
  private final Set<String> exclusions = Sets.newHashSet();

  public static FileQuery on(FileType... types) {
    FileQuery query = new FileQuery();
    for (FileType type : types) {
      query.on("TYPE", type.typeValue());
    }
    return query;
  }

  public static FileQuery onSource() {
    return onMain();
  }

  /**
   * @since 4.2
   */
  public static FileQuery onMain() {
    FileQuery query = new FileQuery();
    return query.on("TYPE", "MAIN");
  }

  public static FileQuery onTest() {
    FileQuery query = new FileQuery();
    return query.on("TYPE", "TEST");
  }

  private FileQuery() {
  }

  public FileQuery on(String attribute, String... values) {
    for (String value : values) {
      attributes.put(attribute, value);
    }
    return this;
  }

  public Map<String, Collection<String>> attributes() {
    return attributes.asMap();
  }

  public Collection<FileType> types() {
    return Collections2.transform(attributes.get("TYPE"), new Function<String, FileType>() {
      @Override
      public FileType apply(@Nullable String input) {
        return input != null ? FileType.valueOf(input) : null;
      }
    });
  }

  public Collection<String> typeAttributes() {
    return attributes.get("TYPE");
  }

  public Collection<String> languages() {
    return attributes.get("LANG");
  }

  public FileQuery onLanguage(String... languages) {
    return on("LANG", languages);
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
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    FileQuery rhs = (FileQuery) obj;
    return new EqualsBuilder()
      .append(attributes, rhs.attributes)
      .append(exclusions, rhs.exclusions)
      .append(inclusions, rhs.inclusions)
      .isEquals();
  }

}
