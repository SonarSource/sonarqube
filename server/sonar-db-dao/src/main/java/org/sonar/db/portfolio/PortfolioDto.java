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
package org.sonar.db.portfolio;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.entity.EntityDto;

public class PortfolioDto extends EntityDto {
  public enum SelectionMode {
    NONE, MANUAL, REGEXP, REST, TAGS
  }

  private String branchKey;

  private String rootUuid;
  private String parentUuid;
  private String selectionMode;
  private String selectionExpression;

  private long createdAt;
  private long updatedAt;

  public String getRootUuid() {
    return rootUuid;
  }

  public PortfolioDto setRootUuid(String rootUuid) {
    this.rootUuid = rootUuid;
    return this;
  }

  @CheckForNull
  public String getParentUuid() {
    return parentUuid;
  }

  public boolean isRoot() {
    return parentUuid == null;
  }

  public PortfolioDto setParentUuid(@Nullable String parentUuid) {
    this.parentUuid = parentUuid;
    return this;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public void setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
  }

  @CheckForNull
  public String getBranchKey() {
    return branchKey;
  }

  public void setBranchKey(@Nullable String branchKey) {
    this.branchKey = branchKey;
  }

  @Override
  public String getQualifier() {
    if (isRoot()) {
      return Qualifiers.VIEW;
    }
    return Qualifiers.SUBVIEW;
  }

  public String getSelectionMode() {
    return selectionMode;
  }

  public PortfolioDto setSelectionMode(String selectionMode) {
    this.selectionMode = selectionMode;
    return this;
  }

  public PortfolioDto setSelectionMode(SelectionMode selectionMode) {
    this.selectionMode = selectionMode.name();
    return this;
  }

  @CheckForNull
  public String getSelectionExpression() {
    return selectionExpression;
  }

  public PortfolioDto setSelectionExpression(@Nullable String selectionExpression) {
    this.selectionExpression = selectionExpression;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public PortfolioDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public PortfolioDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public PortfolioDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * This is the setter used by MyBatis mapper.
   */
  public PortfolioDto setKee(String kee) {
    this.kee = kee;
    return this;
  }

  public PortfolioDto setKey(String key) {
    return setKee(key);
  }

  @Override
  public PortfolioDto setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
    return this;
  }

  public PortfolioDto setName(String name) {
    this.name = name;
    return this;
  }

  public PortfolioDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }
}
