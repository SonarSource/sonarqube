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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.PathUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * @since 4.2
 */
public class DefaultInputFile implements InputFile, Serializable {

  private final String relativePath;
  private final String moduleKey;
  protected Path moduleBaseDir;
  private String language;
  private Type type = Type.MAIN;
  private Status status;
  private int lines;
  private Charset charset;

  public DefaultInputFile(String moduleKey, String relativePath) {
    this.moduleKey = moduleKey;
    this.relativePath = PathUtils.sanitize(relativePath);
  }

  @Override
  public String relativePath() {
    return relativePath;
  }

  @Override
  public String absolutePath() {
    return PathUtils.sanitize(path().toString());
  }

  @Override
  public File file() {
    return path().toFile();
  }

  @Override
  public Path path() {
    if (moduleBaseDir == null) {
      throw new IllegalStateException("Can not return the java.nio.file.Path because module baseDir is not set (see method setModuleBaseDir(java.io.File))");
    }
    return moduleBaseDir.resolve(relativePath);
  }

  @CheckForNull
  @Override
  public String language() {
    return language;
  }

  @Override
  public Type type() {
    return type;
  }

  /**
   * {@link #setStatus(org.sonar.api.batch.fs.InputFile.Status)}
   */
  @Override
  public Status status() {
    return status;
  }

  @Override
  public int lines() {
    return lines;
  }

  /**
   * Component key.
   */
  public String key() {
    return new StringBuilder().append(moduleKey).append(":").append(relativePath).toString();
  }

  public String moduleKey() {
    return moduleKey;
  }

  public Charset charset() {
    return charset;
  }

  /**
   * For testing purpose. Will be automaticall set when file is added to {@link DefaultFileSystem}
   */
  public DefaultInputFile setModuleBaseDir(Path moduleBaseDir) {
    this.moduleBaseDir = moduleBaseDir.normalize();
    return this;
  }

  public DefaultInputFile setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public DefaultInputFile setType(Type type) {
    this.type = type;
    return this;
  }

  public DefaultInputFile setStatus(Status status) {
    this.status = status;
    return this;
  }

  public DefaultInputFile setLines(int lines) {
    this.lines = lines;
    return this;
  }

  public DefaultInputFile setCharset(Charset charset) {
    this.charset = charset;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultInputFile)) {
      return false;
    }

    DefaultInputFile that = (DefaultInputFile) o;
    return moduleKey.equals(that.moduleKey) && relativePath.equals(that.relativePath);
  }

  @Override
  public int hashCode() {
    return moduleKey.hashCode() + relativePath.hashCode() * 13;
  }

  @Override
  public String toString() {
    return "[moduleKey=" + moduleKey + ", relative=" + relativePath + ", basedir=" + moduleBaseDir + "]";
  }

}
