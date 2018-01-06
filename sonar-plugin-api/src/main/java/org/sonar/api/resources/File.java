/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.WildcardPattern;

/**
 * @since 1.10
 * @deprecated since 5.6 replaced by {@link InputFile}.
 */
@Deprecated
public class File extends Resource {

  public static final String SCOPE = Scopes.FILE;

  private String filename;
  private Language language;
  private Directory parent;
  private String qualifier = Qualifiers.FILE;

  protected File() {
    // Used by factory method
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#getParent()
   */
  @Override
  public Directory getParent() {
    return parent;
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#matchFilePattern(String)
   */
  @Override
  public boolean matchFilePattern(String antPattern) {
    WildcardPattern matcher = WildcardPattern.create(antPattern, Directory.SEPARATOR);
    return matcher.match(getKey());
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#getName()
   */
  @Override
  public String getName() {
    return filename;
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#getLongName()
   */
  @Override
  public String getLongName() {
    return StringUtils.defaultIfBlank(getPath(), getKey());
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#getDescription()
   */
  @Override
  public String getDescription() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#getLanguage()
   */
  @Override
  public Language getLanguage() {
    return language;
  }

  /**
   * Sets the language of the file
   */
  public void setLanguage(Language language) {
    this.language = language;
  }

  /**
   * @return SCOPE_ENTITY
   */
  @Override
  public final String getScope() {
    return SCOPE;
  }

  /**
   * Returns the qualifier associated to this File. Should be QUALIFIER_FILE or QUALIFIER_UNIT_TEST_CLASS
   */
  @Override
  public String getQualifier() {
    return qualifier;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  /**
   * Internal use only.
   * @deprecated since 5.1 use {@link FileSystem#inputFile(org.sonar.api.batch.fs.FilePredicate)}
   */
  @Deprecated
  public static File create(String relativePathFromBasedir) {
    File file = new File();
    String normalizedPath = normalize(relativePathFromBasedir);
    file.setKey(normalizedPath);
    file.setPath(normalizedPath);
    String directoryPath;
    if (normalizedPath != null && normalizedPath.contains(Directory.SEPARATOR)) {
      directoryPath = StringUtils.substringBeforeLast(normalizedPath, Directory.SEPARATOR);
      file.filename = StringUtils.substringAfterLast(normalizedPath, Directory.SEPARATOR);
    } else {
      directoryPath = Directory.SEPARATOR;
      file.filename = normalizedPath;
    }
    file.parent = Directory.create(directoryPath);
    return file;
  }

  /**
   * Internal use only.
   * @deprecated since 5.1 use {@link FileSystem#inputFile(org.sonar.api.batch.fs.FilePredicate)}
   */
  @Deprecated
  public static File create(String relativePathFromBasedir, Language language, boolean unitTest) {
    File file = create(relativePathFromBasedir);
    file.setLanguage(language);
    if (unitTest) {
      file.setQualifier(Qualifiers.UNIT_TEST_FILE);
    }
    return file;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("key", getKey())
      .append("path", getPath())
      .append("filename", filename)
      .append("language", language)
      .toString();
  }
}
