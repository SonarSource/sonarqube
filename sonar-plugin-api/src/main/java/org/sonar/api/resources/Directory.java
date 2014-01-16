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
 * Extends JavaPackage to allow smooth migration from JavaPackage to Directory
 */
public class Directory extends JavaPackage {

  public static final String SEPARATOR = "/";
  public static final String ROOT = "[root]";

  Directory() {
    // Used by factory
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
  }

  @Override
  public String getName() {
    return getKey();
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
    return null;
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
    String normalizedKey = key;
    normalizedKey = normalizedKey.replace('\\', '/');
    normalizedKey = StringUtils.trim(normalizedKey);
    normalizedKey = StringUtils.removeStart(normalizedKey, Directory.SEPARATOR);
    normalizedKey = StringUtils.removeEnd(normalizedKey, Directory.SEPARATOR);
    return normalizedKey;
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
      .toString();
  }

}
