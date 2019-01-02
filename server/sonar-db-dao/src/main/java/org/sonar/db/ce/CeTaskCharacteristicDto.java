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
package org.sonar.db.ce;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public class CeTaskCharacteristicDto {

  public static final String BRANCH_KEY = "branch";
  public static final String BRANCH_TYPE_KEY = "branchType";
  public static final String PULL_REQUEST = "pullRequest";
  public static final Set<String> SUPPORTED_KEYS = unmodifiableSet(new HashSet<>(asList(BRANCH_KEY, BRANCH_TYPE_KEY, PULL_REQUEST)));

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
