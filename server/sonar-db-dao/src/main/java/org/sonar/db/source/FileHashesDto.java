/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.source;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class FileHashesDto {
  protected String uuid;
  protected String fileUuid;
  protected String srcHash;
  protected String dataHash;
  protected String revision;
  protected long updatedAt;
  @Nullable
  protected Integer lineHashesVersion;

  public int getLineHashesVersion() {
    return lineHashesVersion != null ? lineHashesVersion : LineHashVersion.WITHOUT_SIGNIFICANT_CODE.getDbValue();
  }

  public String getUuid() {
    return uuid;
  }

  public String getFileUuid() {
    return fileUuid;
  }

  public FileHashesDto setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
    return this;
  }
  @CheckForNull
  public String getDataHash() {
    return dataHash;
  }



  /**
   * MD5 of column BINARY_DATA. Used to know to detect data changes and need for update.
   */
  public FileHashesDto setDataHash(String s) {
    this.dataHash = s;
    return this;
  }

  @CheckForNull
  public String getSrcHash() {
    return srcHash;
  }

  /**
   * Hash of file content. Value is computed by batch.
   */
  public FileHashesDto setSrcHash(@Nullable String srcHash) {
    this.srcHash = srcHash;
    return this;
  }

  public String getRevision() {
    return revision;
  }

  public FileHashesDto setRevision(@Nullable String revision) {
    this.revision = revision;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

}
