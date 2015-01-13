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
package org.sonar.core.source.db;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class FileSourceDto {
  private Long id;
  private String projectUuid;
  private String fileUuid;
  private long createdAt;
  private long updatedAt;
  private String data;
  private String lineHashes;
  private String dataHash;
  private String srcHash;

  public Long getId() {
    return id;
  }

  public FileSourceDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public FileSourceDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getFileUuid() {
    return fileUuid;
  }

  public FileSourceDto setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
    return this;
  }

  @CheckForNull
  public String getData() {
    return data;
  }

  public FileSourceDto setData(@Nullable String data) {
    this.data = data;
    return this;
  }

  @CheckForNull
  public String getLineHashes() {
    return lineHashes;
  }

  public FileSourceDto setLineHashes(@Nullable String lineHashes) {
    this.lineHashes = lineHashes;
    return this;
  }

  public String getDataHash() {
    return dataHash;
  }

  public FileSourceDto setDataHash(String dataHash) {
    this.dataHash = dataHash;
    return this;
  }

  public String getSrcHash() {
    return srcHash;
  }

  public FileSourceDto setSrcHash(String srcHash) {
    this.srcHash = srcHash;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public FileSourceDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public FileSourceDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
