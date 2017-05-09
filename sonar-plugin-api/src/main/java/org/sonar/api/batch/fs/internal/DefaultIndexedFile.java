/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.utils.PathUtils;

/**
 * @since 6.3
 */
public class DefaultIndexedFile extends DefaultInputComponent implements IndexedFile {
  private final String relativePath;
  private final String moduleKey;
  private final Path moduleBaseDir;
  private String language;
  private final Type type;

  /**
   * Testing purposes only!
   */
  public DefaultIndexedFile(String moduleKey, Path moduleBaseDir, String relativePath) {
    this(moduleKey, moduleBaseDir, relativePath, TestInputFileBuilder.nextBatchId());
  }

  public DefaultIndexedFile(String moduleKey, Path moduleBaseDir, String relativePath, int batchId) {
    this(moduleKey, moduleBaseDir, relativePath, Type.MAIN, batchId);
  }

  public DefaultIndexedFile(String moduleKey, Path moduleBaseDir, String relativePath, Type type, int batchId) {
    super(batchId);
    this.moduleKey = moduleKey;
    this.relativePath = PathUtils.sanitize(relativePath);
    this.moduleBaseDir = moduleBaseDir.normalize();
    this.type = type;
  }

  public DefaultIndexedFile setLanguage(@Nullable String language) {
    this.language = language;
    return this;
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
    return moduleBaseDir.resolve(relativePath);
  }

  @Override
  public InputStream inputStream() throws IOException {
    return Files.newInputStream(path());
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
   * Component key (without branch).
   */
  @Override
  public String key() {
    return new StringBuilder().append(moduleKey).append(":").append(relativePath).toString();
  }

  public String moduleKey() {
    return moduleKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof DefaultIndexedFile)) {
      return false;
    }

    DefaultIndexedFile that = (DefaultIndexedFile) o;
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

  @Override
  public boolean isFile() {
    return true;
  }
}
