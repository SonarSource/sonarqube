/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.qualityprofile;

import java.util.Collections;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.utils.KeyValueFormat;

public class QProfileChangeDto {

  private String uuid;
  private String rulesProfileUuid;
  private String changeType;
  private String userUuid;
  private String data;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  public QProfileChangeDto setUuid(String s) {
    this.uuid = s;
    return this;
  }

  public String getRulesProfileUuid() {
    return rulesProfileUuid;
  }

  public QProfileChangeDto setRulesProfileUuid(String s) {
    this.rulesProfileUuid = s;
    return this;
  }

  public String getChangeType() {
    return changeType;
  }

  public QProfileChangeDto setChangeType(String s) {
    this.changeType = s;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public QProfileChangeDto setCreatedAt(long l) {
    this.createdAt = l;
    return this;
  }

  @CheckForNull
  public String getUserUuid() {
    return userUuid;
  }

  public QProfileChangeDto setUserUuid(@Nullable String s) {
    this.userUuid = s;
    return this;
  }

  @CheckForNull
  public String getData() {
    return data;
  }

  public Map<String, String> getDataAsMap() {
    if (data == null) {
      return Collections.emptyMap();
    }
    return KeyValueFormat.parse(data);
  }

  public QProfileChangeDto setData(@Nullable String csv) {
    this.data = csv;
    return this;
  }

  public QProfileChangeDto setData(@Nullable Map m) {
    if (m == null || m.isEmpty()) {
      this.data = null;
    } else {
      this.data = KeyValueFormat.format(m);
    }
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
