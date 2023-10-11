/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.ce;

import java.util.Set;

import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_URL;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;

public class CeTaskCharacteristicDto {


  public static final Set<String> SUPPORTED_KEYS = Set.of(BRANCH, BRANCH_TYPE, PULL_REQUEST, DEVOPS_PLATFORM_URL, DEVOPS_PLATFORM_PROJECT_IDENTIFIER);

  private String uuid;
  private String taskUuid;
  private String key;
  private String value;

  public String getUuid() {
    return uuid;
  }

  public CeTaskCharacteristicDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getTaskUuid() {
    return taskUuid;
  }

  public CeTaskCharacteristicDto setTaskUuid(String taskUuid) {
    this.taskUuid = taskUuid;
    return this;
  }

  public String getKey() {
    return key;
  }

  public CeTaskCharacteristicDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public CeTaskCharacteristicDto setValue(String value) {
    this.value = value;
    return this;
  }
}
