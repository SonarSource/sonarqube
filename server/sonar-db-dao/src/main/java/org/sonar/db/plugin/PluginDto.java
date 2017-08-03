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
package org.sonar.db.plugin;

import javax.annotation.CheckForNull;
import org.apache.commons.lang.builder.ToStringBuilder;

public class PluginDto {
  /** Technical unique identifier, can't be null */
  private String uuid;
  /** Plugin key, unique, can't be null */
  private String kee;
  /** Base plugin key, can be null */
  private String basePluginKey;
  /** JAR file MD5 checksum, can't be null */
  private String hash;
  /** Time plugin was first installed */
  private long createdAt;
  /** Time of last plugin update (=md5 change) */
  private long updatedAt;

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

  public PluginDto setBasePluginKey(String s) {
    this.basePluginKey = s;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public PluginDto setHash(String s) {
    this.hash = s;
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

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("uuid", uuid)
      .append("key", kee)
      .append("basePluginKey", basePluginKey)
      .append("jarMd5", hash)
      .append("createdAt", createdAt)
      .append("updatedAt", updatedAt)
      .toString();
  }
}
