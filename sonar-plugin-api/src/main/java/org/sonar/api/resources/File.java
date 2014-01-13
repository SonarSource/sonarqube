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
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.WildcardPattern;

import java.util.List;

/**
 * This class is an implementation of a resource of type FILE
 *
 * @since 1.10
 */
public class File extends Resource {

  public static final String SCOPE = Scopes.FILE;

  private String directoryDeprecatedKey;
  private String filename;
  private Language language;
  private Directory parent;
  private String qualifier = Qualifiers.FILE;

  private File() {
    // Used by factory method
  }

  /**
   * File in project. Key is the path relative to project source directories. It is not the absolute path and it does not include the path
   * to source directories. Example : <code>new File("org/sonar/foo.sql")</code>. The absolute path may be
   * c:/myproject/src/main/sql/org/sonar/foo.sql. Project root is c:/myproject and source dir is src/main/sql.
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public File(String deprecatedKey) {
    if (deprecatedKey == null) {
      throw new IllegalArgumentException("File key is null");
    }
    String realKey = parseKey(deprecatedKey);
    if (realKey.indexOf(Directory.SEPARATOR) >= 0) {
      this.directoryDeprecatedKey = Directory.parseKey(StringUtils.substringBeforeLast(deprecatedKey, Directory.SEPARATOR));
      this.filename = StringUtils.substringAfterLast(realKey, Directory.SEPARATOR);
      realKey = new StringBuilder().append(this.directoryDeprecatedKey).append(Directory.SEPARATOR).append(filename).toString();

    } else {
      this.filename = deprecatedKey;
    }
    setDeprecatedKey(realKey);
  }

  /**
   * Creates a file from its containing directory and name
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public File(String deprecatedDirectoryKey, String filename) {
    this.filename = StringUtils.trim(filename);
    if (StringUtils.isBlank(deprecatedDirectoryKey)) {
      setDeprecatedKey(filename);

    } else {
      this.directoryDeprecatedKey = Directory.parseKey(deprecatedDirectoryKey);
      setDeprecatedKey(new StringBuilder().append(directoryDeprecatedKey).append(Directory.SEPARATOR).append(this.filename).toString());
    }
  }

  /**
   * Creates a File from its language and its key
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public File(Language language, String deprecatedKey) {
    this(deprecatedKey);
    this.language = language;
  }

  /**
   * Creates a File from language, directory and filename
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public File(Language language, String directory, String filename) {
    this(directory, filename);
    this.language = language;
  }

  /**
   * {@inheritDoc}
   *
   * @see Resource#getParent()
   */
  @Override
  public Directory getParent() {
    if (parent == null) {
      parent = new Directory(directoryDeprecatedKey);
    }
    return parent;
  }

  private static String parseKey(String key) {
    if (StringUtils.isBlank(key)) {
      return null;
    }

    key = key.replace('\\', '/');
    key = StringUtils.trim(key);
    return key;
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
   * Creates a File from an io.file and a list of sources directories
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public static File fromIOFile(java.io.File file, List<java.io.File> sourceDirs) {
    PathResolver.RelativePath relativePath = new PathResolver().relativePath(sourceDirs, file);
    if (relativePath != null) {
      return new File(relativePath.path());
    }
    return null;
  }

  /**
   * Creates a File from its name and a project
   * @deprecated since 4.2 use {@link #create(String, String, Language, boolean)}
   */
  @Deprecated
  public static File fromIOFile(java.io.File file, Project project) {
    return fromIOFile(file, project.getFileSystem().getSourceDirs());
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
    return getKey();
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
   * Returns the qualifier associated to this File. Should be QUALIFIER_FILE or
   *
   * @return QUALIFIER_UNIT_TEST_CLASS
   */
  @Override
  public String getQualifier() {
    return qualifier;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  public static File create(String relativePathFromBasedir, String relativePathFromSourceDir, Language language, boolean unitTest) {
    File file = new File();
    String normalizedPath = normalize(relativePathFromBasedir);
    file.setKey(normalizedPath);
    file.setPath(normalizedPath);
    String directoryKey = StringUtils.substringBeforeLast(normalizedPath, Directory.SEPARATOR);
    file.setLanguage(language);
    if (relativePathFromSourceDir.contains(Directory.SEPARATOR)) {
      file.filename = StringUtils.substringAfterLast(relativePathFromSourceDir, Directory.SEPARATOR);
      file.directoryDeprecatedKey = Directory.parseKey(StringUtils.substringBeforeLast(relativePathFromSourceDir, Directory.SEPARATOR));
      file.setDeprecatedKey(file.directoryDeprecatedKey + Directory.SEPARATOR + file.filename);
    } else {
      file.filename = relativePathFromSourceDir;
      file.directoryDeprecatedKey = Directory.ROOT;
      file.setDeprecatedKey(file.filename);
    }
    if (unitTest) {
      file.setQualifier(Qualifiers.UNIT_TEST_FILE);
    }
    file.parent = Directory.create(directoryKey, file.directoryDeprecatedKey);
    return file;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("key", getKey())
      .append("path", getPath())
      .append("dir", directoryDeprecatedKey)
      .append("filename", filename)
      .append("language", language)
      .toString();
  }
}
