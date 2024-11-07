/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.utils.PathUtils;

/**
 * @since 6.3
 */
@Immutable
public class DefaultIndexedFile extends DefaultInputComponent implements IndexedFile {
  // should match limit in org.sonar.core.component.ComponentKeys
  private static final Integer MAX_KEY_LENGTH = 400;
  private static final AtomicInteger intGenerator = new AtomicInteger(0);

  private final String projectRelativePath;
  private final String moduleRelativePath;
  private final String projectKey;
  private final String language;
  private final Type type;
  private final Path absolutePath;
  private final SensorStrategy sensorStrategy;
  private final String oldRelativeFilePath;

  /**
   * Testing purposes only!
   */
  public DefaultIndexedFile(String projectKey, Path baseDir, String relativePath, @Nullable String language) {
    this(baseDir.resolve(relativePath), projectKey, relativePath, relativePath, Type.MAIN, language, intGenerator.getAndIncrement(),
      new SensorStrategy(), null);
  }

  public DefaultIndexedFile(Path absolutePath, String projectKey, String projectRelativePath, String moduleRelativePath, Type type, @Nullable String language, int batchId,
    SensorStrategy sensorStrategy) {
    this(absolutePath, projectKey, projectRelativePath, moduleRelativePath, type, language, batchId, sensorStrategy, null);
  }

  public DefaultIndexedFile(Path absolutePath, String projectKey, String projectRelativePath, String moduleRelativePath, Type type, @Nullable String language, int batchId,
    SensorStrategy sensorStrategy, @Nullable String oldRelativeFilePath) {
    super(batchId);
    this.projectKey = projectKey;
    this.projectRelativePath = checkSanitize(projectRelativePath);
    this.moduleRelativePath = PathUtils.sanitize(moduleRelativePath);
    this.type = type;
    this.language = language;
    this.sensorStrategy = sensorStrategy;
    this.absolutePath = absolutePath;
    this.oldRelativeFilePath = oldRelativeFilePath;
    validateKeyLength();
  }

  static String checkSanitize(String relativePath) {
    String sanitized = PathUtils.sanitize(relativePath);
    if(sanitized == null) {
      throw new IllegalArgumentException(String.format("The path '%s' must sanitize to a non-null value", relativePath));
    }
    return sanitized;
  }

  private void validateKeyLength() {
    String key = key();
    if (key.length() > MAX_KEY_LENGTH) {
      throw new IllegalStateException(String.format("Component key (%s) length (%s) is longer than the maximum authorized (%s)", key, key.length(), MAX_KEY_LENGTH));
    }
  }

  @Override
  public String relativePath() {
    return sensorStrategy.isGlobal() ? projectRelativePath : moduleRelativePath;
  }

  public String getModuleRelativePath() {
    return moduleRelativePath;
  }

  public String getProjectRelativePath() {
    return projectRelativePath;
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
    return absolutePath;
  }

  @CheckForNull
  public String oldRelativePath() {
    return oldRelativeFilePath;
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
    return String.join(":", projectKey, projectRelativePath);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || o.getClass() != this.getClass()) {
      return false;
    }

    DefaultIndexedFile that = (DefaultIndexedFile) o;
    return projectRelativePath.equals(that.projectRelativePath);
  }

  @Override
  public int hashCode() {
    return projectRelativePath.hashCode();
  }

  @Override
  public String toString() {
    return projectRelativePath;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public String filename() {
    return path().getFileName().toString();
  }

  @Override
  public URI uri() {
    return path().toUri();
  }
}
