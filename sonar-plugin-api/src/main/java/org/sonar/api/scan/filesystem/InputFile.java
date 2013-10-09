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
package org.sonar.api.scan.filesystem;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @since 4.0
 */
public class InputFile implements Serializable {

  // TODO refactor attribute constants as classes or enums ?

  /**
   * Path relative to module base directory.
   */
  public static final String ATTRIBUTE_BASE_RELATIVE_PATH = "baseRelPath";

  // TODO ambiguity of term "source" with sourceDir versus testDir properties
  // Here it does not depend on type.
  public static final String ATTRIBUTE_SOURCEDIR_PATH = "srcDirPath";
  public static final String ATTRIBUTE_SOURCE_RELATIVE_PATH = "srcRelPath";

  public static final String ATTRIBUTE_CANONICAL_PATH = "canonicalPath";
  public static final String ATTRIBUTE_LANGUAGE = "lang";
  public static final String ATTRIBUTE_TYPE = "type";
  public static final String ATTRIBUTE_STATUS = "status";
  public static final String STATUS_SAME = "same";
  public static final String STATUS_CHANGED = "changed";
  public static final String STATUS_ADDED = "added";
  public static final String ATTRIBUTE_HASH = "checksum";
  public static final String ATTRIBUTE_EXTENSION = "extension";
  public static final String TYPE_SOURCE = "source";
  public static final String TYPE_TEST = "test";

  // TODO limitation of persistit -> add unit test
  private transient File transientFile;
  private Map<String, String> attributes;

  private InputFile(File file, Map<String, String> attributes) {
    this.transientFile = file;
    this.attributes = attributes;
  }

  /**
   * Plugins should not build their own instances of {@link InputFile}. This method
   * aims to be used by unit tests.
   * // TODO provide builder ?
   */
  public static InputFile create(File file, String baseRelativePath, Map<String, String> attributes) {
    Map<String,String> copy = Maps.newHashMap(attributes);
    copy.put(InputFile.ATTRIBUTE_BASE_RELATIVE_PATH, baseRelativePath);
    return new InputFile(file, copy);
  }

  /**
   * Path from module base directory. Path is unique and identifies file within given
   * <code>{@link ModuleFileSystem}</code>. File separator is the forward slash ('/'),
   * even on MSWindows.
   */
  public String path() {
    return attribute(ATTRIBUTE_BASE_RELATIVE_PATH);
  }

  public File file() {
    if (transientFile == null) {
      transientFile = new File(attribute(ATTRIBUTE_CANONICAL_PATH));
    }
    return transientFile;
  }

  public String name() {
    return file().getName();
  }

  public boolean has(String attribute, String value) {
    return StringUtils.equals(attributes.get(attribute), value);
  }

  @CheckForNull
  public String attribute(String key) {
    return attributes.get(key);
  }

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
    InputFile inputFile = (InputFile) o;
    return attribute(ATTRIBUTE_CANONICAL_PATH).equals(inputFile.attribute(ATTRIBUTE_CANONICAL_PATH));
  }

  @Override
  public int hashCode() {
    return path().hashCode();
  }

  public static List<File> toFiles(Iterable<InputFile> inputFiles) {
    List<File> files = Lists.newArrayList();
    for (InputFile inputFile : inputFiles) {
      files.add(inputFile.file());
    }
    return files;
  }


}
