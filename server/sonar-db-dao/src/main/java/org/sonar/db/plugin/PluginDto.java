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
package org.sonar.db.plugin;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class PluginDto {
  /** Technical unique identifier, can't be null */
  private String uuid = null;
  /** Plugin key, unique, can't be null */
  private String kee = null;
  /** Base plugin key, can be null */
  private String basePluginKey = null;
  /** JAR file MD5 checksum, can't be null */
  private String fileHash = null;
  // core feature or not
  private Type type = null;
  /** whether the plugin has been removed **/
  private boolean removed = false;
  /** Time plugin was first installed */
  private long createdAt = 0L;
  /** Time of last plugin update (=md5 change) */
  private long updatedAt = 0L;

  public String getUuid() {
    return uuid;
  }

  public PluginDto setUuid(String s) {
    this.uuid = s;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public PluginDto setKee(String s) {
    this.kee = s;
    return this;
  }

  @CheckForNull
  public String getBasePluginKey() {
    return basePluginKey;
  }

  public PluginDto setBasePluginKey(@Nullable String s) {
    this.basePluginKey = s;
    return this;
  }

  public String getFileHash() {
    return fileHash;
  }

  public PluginDto setFileHash(String s) {
    this.fileHash = s;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public PluginDto setCreatedAt(long l) {
    this.createdAt = l;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public PluginDto setUpdatedAt(long l) {
    this.updatedAt = l;
    return this;
  }

  public Type getType() {
    return type;
  }

  public PluginDto setType(Type type) {
    this.type = type;
    return this;
  }

  public boolean isRemoved() {
    return removed;
  }

  public PluginDto setRemoved(boolean removed) {
    this.removed = removed;
    return this;
  }

  public enum Type {
    BUNDLED,
    EXTERNAL
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("uuid", uuid)
      .append("key", kee)
      .append("basePluginKey", basePluginKey)
      .append("fileHash", fileHash)
      .append("removed", removed)
      .append("createdAt", createdAt)
      .append("updatedAt", updatedAt)
      .toString();
  }
}
