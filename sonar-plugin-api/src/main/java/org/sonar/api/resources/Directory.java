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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.utils.WildcardPattern;

/**
 * @since 1.10
 */
public class Directory extends Resource {

  public static final String SEPARATOR = "/";
  public static final String ROOT = "[root]";

  private Language language;

  private Directory() {
    // USed by factory
  }

  /**
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public Directory(String deprecatedKey) {
    this(deprecatedKey, null);
  }

  /**
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public Directory(String deprecatedKey, Language language) {
    setDeprecatedKey(parseKey(deprecatedKey));
    this.language = language;
  }

  @Override
  public String getName() {
    return getDeprecatedKey();
  }

  @Override
  public String getLongName() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public Language getLanguage() {
    return language;
  }

  @Override
  public String getScope() {
    return Scopes.DIRECTORY;
  }

  @Override
  public String getQualifier() {
    return Qualifiers.DIRECTORY;
  }

  @Override
  public Resource getParent() {
    return null;
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    WildcardPattern matcher = WildcardPattern.create(antPattern, "/");
    return matcher.match(getKey());
  }

  public static String parseKey(String key) {
    if (StringUtils.isBlank(key)) {
      return ROOT;
    }

    key = key.replace('\\', '/');
    key = StringUtils.trim(key);
    key = StringUtils.removeStart(key, Directory.SEPARATOR);
    key = StringUtils.removeEnd(key, Directory.SEPARATOR);
    return key;
  }

  public static Directory create(String path, String directoryDeprecatedKey) {
    Directory d = new Directory();
    String normalizedPath = normalize(path);
    d.setKey(normalizedPath);
    d.setDeprecatedKey(parseKey(directoryDeprecatedKey));
    d.setPath(normalizedPath);
    return d;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("key", getKey())
      .append("deprecatedKey", getDeprecatedKey())
      .append("path", getPath())
      .append("language", language)
      .toString();
  }

}
