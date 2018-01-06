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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.utils.PathUtils;

/**
 * @since 4.5
 */
public class DefaultInputDir extends DefaultInputComponent implements InputDir {

  private final String relativePath;
  private final String moduleKey;
  private Path moduleBaseDir;

  public DefaultInputDir(String moduleKey, String relativePath) {
    this(moduleKey, relativePath, TestInputFileBuilder.nextBatchId());
  }

  public DefaultInputDir(String moduleKey, String relativePath, int batchId) {
    super(batchId);
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

  public String moduleKey() {
    return moduleKey;
  }

  @Override
  public String key() {
    StringBuilder sb = new StringBuilder().append(moduleKey).append(":");
    if (StringUtils.isEmpty(relativePath)) {
      sb.append("/");
    } else {
      sb.append(relativePath);
    }
    return sb.toString();
  }

  /**
   * For testing purpose. Will be automatically set when dir is added to {@link DefaultFileSystem}
   */
  public DefaultInputDir setModuleBaseDir(Path moduleBaseDir) {
    this.moduleBaseDir = moduleBaseDir.normalize();
    return this;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    DefaultInputDir that = (DefaultInputDir) o;
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
  public URI uri() {
    return path().toUri();
  }
}
