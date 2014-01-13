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
package org.sonar.api.scan.filesystem.internal;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.PathUtils;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * PLUGINS MUST NOT USE THIS CLASS, EVEN FOR UNIT TESTING.
 *
 * @since 4.0
 */
public class DefaultInputFile implements InputFile {

  /**
   * We're not sure that this is the correct way, so not in API yet.
   */
  public static final String ATTRIBUTE_COMPONENT_KEY = "CMP_KEY";

  public static final String ATTRIBUTE_COMPONENT_DEPRECATED_KEY = "CMP_DEPRECATED_KEY";

  private final String absolutePath;
  private final String path;
  private final Map<String, String> attributes;
  private final String encoding;

  private DefaultInputFile(File file, Charset encoding, String path, Map<String, String> attributes) {
    this.encoding = encoding.name();
    this.absolutePath = PathUtils.canonicalPath(file);
    this.path = FilenameUtils.separatorsToUnix(path);
    this.attributes = attributes;
  }

  /**
   * Plugins must not build their own instances of {@link InputFile}.
   * {@link org.sonar.api.scan.filesystem.ModuleFileSystem} must be used to search for files to scan.
   * <p/>
   * Usage: <code>InputFile.create(file, "src/main/java/com/Foo.java", attributes)</code>
   */
  public static DefaultInputFile create(File file, Charset encoding, String path, Map<String, String> attributes) {
    return new DefaultInputFile(file, encoding, path, attributes);
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String absolutePath() {
    return absolutePath;
  }

  @Override
  public File file() {
    return new File(absolutePath);
  }

  @Override
  public Charset encoding() {
    return Charsets.toCharset(encoding);
  }

  @Override
  public String name() {
    return file().getName();
  }

  @Override
  public String type() {
    return attribute(ATTRIBUTE_TYPE);
  }

  @Override
  public boolean has(String attribute, String value) {
    return StringUtils.equals(attributes.get(attribute), value);
  }

  @Override
  @CheckForNull
  public String attribute(String key) {
    return attributes.get(key);
  }

  @Override
  public Map<String, String> attributes() {
    return attributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultInputFile other = (DefaultInputFile) o;
    return absolutePath.equals(other.absolutePath);
  }

  @Override
  public int hashCode() {
    return absolutePath.hashCode();
  }

  @Override
  public String toString() {
    return String.format("[%s,%s]", path, type());
  }
}
