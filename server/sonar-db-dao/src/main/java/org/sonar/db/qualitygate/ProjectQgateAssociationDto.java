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
package org.sonar.db.qualitygate;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.3
 */
public class ProjectQgateAssociationDto {

  private String uuid;
  private String key;
  private String name;
  private String gateUuid;
  private boolean containsAiCode;
  private boolean aiCodeSupportedByQg;

  public ProjectQgateAssociationDto() {
    // do nothing
  }

  public String getUuid() {
    return uuid;
  }

  public ProjectQgateAssociationDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getKey() {
    return key;
  }

  public ProjectQgateAssociationDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public ProjectQgateAssociationDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getGateUuid() {
    return gateUuid;
  }

  public ProjectQgateAssociationDto setGateUuid(@Nullable String gateUuid) {
    this.gateUuid = gateUuid;
    return this;
  }

  public boolean getContainsAiCode() {
    return containsAiCode;
  }

  public ProjectQgateAssociationDto setContainsAiCode(boolean containsAiCode) {
    this.containsAiCode = containsAiCode;
    return this;
  }

  public boolean isAiCodeSupportedByQg() {
    return aiCodeSupportedByQg;
  }

  public void setAiCodeSupportedByQg(boolean aiCodeSupportedByQg) {
    this.aiCodeSupportedByQg = aiCodeSupportedByQg;
  }
}
