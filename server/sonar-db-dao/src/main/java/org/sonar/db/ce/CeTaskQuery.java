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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.DatabaseUtils;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Db Query used for CE_QUEUE and CE_ACTIVITY tables
 */
public class CeTaskQuery {

  public static final int MAX_COMPONENT_UUIDS = DatabaseUtils.PARTITION_SIZE_FOR_ORACLE;

  private boolean onlyCurrents = false;
  // SONAR-7681 a public implementation of List must be used in MyBatis - potential concurrency exceptions otherwise
  @Nullable
  private ArrayList<String> mainComponentUuids;
  @Nullable
  private ArrayList<String> statuses;
  @Nullable
  private String type;
  @Nullable
  private Long minSubmittedAt;
  @Nullable
  private Long maxExecutedAt;
  @Nullable
  private Long minExecutedAt;
  @Nullable
  private ArrayList<String> errorTypes;

  @CheckForNull
  public List<String> getMainComponentUuids() {
    return mainComponentUuids;
  }

  public CeTaskQuery setMainComponentUuids(@Nullable List<String> l) {
    this.mainComponentUuids = l == null ? null : newArrayList(l);
    return this;
  }

  public boolean isShortCircuitedByMainComponentUuids() {
    return mainComponentUuids != null && (mainComponentUuids.isEmpty() || mainComponentUuids.size() > MAX_COMPONENT_UUIDS);
  }

  public CeTaskQuery setMainComponentUuid(@Nullable String s) {
    if (s == null) {
      this.mainComponentUuids = null;
    } else {
      this.mainComponentUuids = newArrayList(s);
    }
    return this;
  }

  public boolean isOnlyCurrents() {
    return onlyCurrents;
  }

  public CeTaskQuery setOnlyCurrents(boolean onlyCurrents) {
    this.onlyCurrents = onlyCurrents;
    return this;
  }

  @CheckForNull
  public List<String> getStatuses() {
    return statuses;
  }

  public CeTaskQuery setStatuses(@Nullable List<String> statuses) {
    this.statuses = statuses == null ? null : newArrayList(statuses);
    return this;
  }

  @CheckForNull
  public List<String> getErrorTypes() {
    return errorTypes;
  }

  public CeTaskQuery setErrorTypes(@Nullable List<String> l) {
    this.errorTypes = l == null ? null : newArrayList(l);
    return this;
  }

  @CheckForNull
  public String getType() {
    return type;
  }

  public CeTaskQuery setType(@Nullable String type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public Long getMaxExecutedAt() {
    return maxExecutedAt;
  }

  public CeTaskQuery setMaxExecutedAt(@Nullable Long l) {
    this.maxExecutedAt = l;
    return this;
  }

  @CheckForNull
  public Long getMinExecutedAt() {
    return minExecutedAt;
  }

  public CeTaskQuery setMinExecutedAt(@Nullable Long l) {
    this.minExecutedAt = l;
    return this;
  }

  @CheckForNull
  public Long getMinSubmittedAt() {
    return minSubmittedAt;
  }

  public CeTaskQuery setMinSubmittedAt(@Nullable Long l) {
    this.minSubmittedAt = l;
    return this;
  }
}
