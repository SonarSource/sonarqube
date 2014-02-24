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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.WildcardPattern;

import javax.annotation.CheckForNull;

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
   * @deprecated since 4.2 use {@link #fromIOFile(java.io.File, Project)}
   */
  @Deprecated
  public File(String relativePathFromSourceDir) {
    if (relativePathFromSourceDir == null) {
      throw new IllegalArgumentException("File key is null");
    }
    String realKey = parseKey(relativePathFromSourceDir);
    if (realKey.indexOf(Directory.SEPARATOR) >= 0) {
      this.directoryDeprecatedKey = Directory.parseKey(StringUtils.substringBeforeLast(relativePathFromSourceDir, Directory.SEPARATOR));
      this.filename = StringUtils.substringAfterLast(realKey, Directory.SEPARATOR);
      realKey = new StringBuilder().append(this.directoryDeprecatedKey).append(Directory.SEPARATOR).append(filename).toString();

    } else {
      this.filename = relativePathFromSourceDir;
    }
    setDeprecatedKey(realKey);
  }

  /**
   * Creates a file from its containing directory and name
   * @deprecated since 4.2 use {@link #fromIOFile(java.io.File, Project)}
   */
  @Deprecated
  public File(String relativeDirectoryPathFromSourceDir, String filename) {
    this.filename = StringUtils.trim(filename);
    if (StringUtils.isBlank(relativeDirectoryPathFromSourceDir)) {
      setDeprecatedKey(filename);

    } else {
      this.directoryDeprecatedKey = Directory.parseKey(relativeDirectoryPathFromSourceDir);
      setDeprecatedKey(new StringBuilder().append(directoryDeprecatedKey).append(Directory.SEPARATOR).append(this.filename).toString());
    }
  }

  /**
   * Creates a File from its language and its key
   * @deprecated since 4.2 use {@link #fromIOFile(java.io.File, Project)}
   */
  @Deprecated
  public File(Language language, String relativePathFromSourceDir) {
    this(relativePathFromSourceDir);
    this.language = language;
  }

  /**
   * Creates a File from language, directory and filename
   * @deprecated since 4.2 use {@link #fromIOFile(java.io.File, Project)}
   */
  @Deprecated
  public File(Language language, String relativeDirectoryPathFromSourceDir, String filename) {
    this(relativeDirectoryPathFromSourceDir, filename);
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
    String normalizedKey = key;
    normalizedKey = normalizedKey.replace('\\', '/');
    normalizedKey = StringUtils.trim(normalizedKey);
    return normalizedKey;
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
   * @deprecated since 4.2 use {@link #fromIOFile(java.io.File, Project)}
   */
  @Deprecated
  @CheckForNull
  public static File fromIOFile(java.io.File file, List<java.io.File> sourceDirs) {
    PathResolver.RelativePath relativePath = new PathResolver().relativePath(sourceDirs, file);
    if (relativePath != null) {
      return new File(relativePath.path());
    }
    return null;
  }

  /**
   * Creates a {@link File} from an absolute {@link java.io.File} and a module.
   * The returned {@link File} can be then passed for example to
   * {@link SensorContext#saveMeasure(Resource, org.sonar.api.measures.Measure)}.
   * @param file absolute path to a file
   * @param module
   * @return null if the file is not under module basedir.
   */
  @CheckForNull
  public static File fromIOFile(java.io.File file, Project module) {
    String relativePathFromBasedir = new PathResolver().relativePath(module.getFileSystem().getBasedir(), file);
    if (relativePathFromBasedir != null) {
      return File.create(relativePathFromBasedir);
    }
    return null;
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
   * Create a File that is partially initialized. But that's enough to call for example
   * {@link SensorContext#saveMeasure(Resource, org.sonar.api.measures.Measure)} when resources are already indexed.
   * Internal use only.
   * @since 4.2
   */
  public static File create(String relativePathFromBasedir) {
    File file = new File();
    String normalizedPath = normalize(relativePathFromBasedir);
    file.setKey(normalizedPath);
    file.setPath(normalizedPath);
    String directoryPath;
    if (normalizedPath != null && normalizedPath.contains(Directory.SEPARATOR)) {
      directoryPath = StringUtils.substringBeforeLast(normalizedPath, Directory.SEPARATOR);
    } else {
      directoryPath = Directory.SEPARATOR;
    }
    file.parent = Directory.create(directoryPath);
    return file;
  }

  /**
   * Create a file that is fully initialized. Use for indexing resources.
   * Internal use only.
   * @since 4.2
   */
  public static File create(String relativePathFromBasedir, String relativePathFromSourceDir, Language language, boolean unitTest) {
    File file = create(relativePathFromBasedir);
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
    file.parent.setDeprecatedKey(file.directoryDeprecatedKey);
    return file;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("key", getKey())
      .append("deprecatedKey", getDeprecatedKey())
      .append("path", getPath())
      .append("dir", directoryDeprecatedKey)
      .append("filename", filename)
      .append("language", language)
      .toString();
  }
}
