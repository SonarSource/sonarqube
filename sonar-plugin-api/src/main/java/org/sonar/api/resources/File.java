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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.WildcardPattern;

import javax.annotation.CheckForNull;

/**
 * This class is an implementation of a resource of type FILE
 *
 * @since 1.10
 */
public class File extends Resource {

  public static final String SCOPE = Scopes.FILE;

  private String filename;
  private Language language;
  private Directory parent;
  private String qualifier = Qualifiers.FILE;

  private File() {
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
   * Creates a {@link File} from an absolute {@link java.io.File} and a module.
   * The returned {@link File} can be then passed for example to
   * {@link SensorContext#saveMeasure(Resource, org.sonar.api.measures.Measure)}.
   * @param file absolute path to a file
   * @param module
   * @return null if the file is not under module basedir.
   * @deprecated since 4.5 use {@link FileSystem#inputFile(org.sonar.api.batch.fs.FilePredicate)}
   */
  @Deprecated
  @CheckForNull
  public static File fromIOFile(java.io.File file, Project module) {
    String relativePathFromBasedir = new PathResolver().relativePath(module.getBaseDir(), file);
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
