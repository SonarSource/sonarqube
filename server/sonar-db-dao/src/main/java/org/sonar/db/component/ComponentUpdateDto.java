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
package org.sonar.db.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ComponentUpdateDto {
  private String uuid;

  /**
   * if true, the component is being updated
   * See https://jira.sonarsource.com/browse/SONAR-7700
   */
  private boolean bChanged;
  /**
   * Component keys are normally immutable. But in SQ 7.6 we have to migrate component keys to drop modules.
   */
  private String bKey;
  private String bCopyComponentUuid;
  private String bDescription;
  private boolean bEnabled;
  private String bUuidPath;
  private String bLanguage;
  private String bLongName;
  private String bModuleUuid;
  private String bModuleUuidPath;
  private String bName;
  private String bPath;
  private String bQualifier;

  public ComponentUpdateDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public boolean isBChanged() {
    return bChanged;
  }

  public String getBKey() {
    return bKey;
  }

  @CheckForNull
  public String getBCopyComponentUuid() {
    return bCopyComponentUuid;
  }

  @CheckForNull
  public String getBDescription() {
    return bDescription;
  }

  public boolean isBEnabled() {
    return bEnabled;
  }

  public String getBUuidPath() {
    return bUuidPath;
  }

  @CheckForNull
  public String getBLanguage() {
    return bLanguage;
  }

  @CheckForNull
  public String getBLongName() {
    return bLongName;
  }

  @CheckForNull
  public String getBModuleUuid() {
    return bModuleUuid;
  }

  @CheckForNull
  public String getBModuleUuidPath() {
    return bModuleUuidPath;
  }

  @CheckForNull
  public String getBName() {
    return bName;
  }

  @CheckForNull
  public String getBPath() {
    return bPath;
  }

  @CheckForNull
  public String getBQualifier() {
    return bQualifier;
  }

  public ComponentUpdateDto setBChanged(boolean b) {
    this.bChanged = b;
    return this;
  }

  public ComponentUpdateDto setBKey(String s) {
    this.bKey = s;
    return this;
  }

  public ComponentUpdateDto setBCopyComponentUuid(@Nullable String s) {
    this.bCopyComponentUuid = s;
    return this;
  }

  public ComponentUpdateDto setBEnabled(boolean b) {
    this.bEnabled = b;
    return this;
  }

  public ComponentUpdateDto setBUuidPath(String bUuidPath) {
    this.bUuidPath = bUuidPath;
    return this;
  }

  public ComponentUpdateDto setBName(@Nullable String s) {
    this.bName = s;
    return this;
  }

  public ComponentUpdateDto setBLongName(@Nullable String s) {
    this.bLongName = s;
    return this;
  }

  public ComponentUpdateDto setBDescription(@Nullable String s) {
    this.bDescription = s;
    return this;
  }

  public ComponentUpdateDto setBModuleUuid(@Nullable String s) {
    this.bModuleUuid = s;
    return this;
  }

  public ComponentUpdateDto setBModuleUuidPath(@Nullable String s) {
    this.bModuleUuidPath = s;
    return this;
  }

  public ComponentUpdateDto setBPath(@Nullable String s) {
    this.bPath = s;
    return this;
  }

  public ComponentUpdateDto setBLanguage(@Nullable String s) {
    this.bLanguage = s;
    return this;
  }

  public ComponentUpdateDto setBQualifier(@Nullable String s) {
    this.bQualifier = s;
    return this;
  }

  /**
   * Copy the A-fields to B-fields. The field bChanged is kept to false.
   */
  public static ComponentUpdateDto copyFrom(ComponentDto from) {
    return new ComponentUpdateDto()
      .setUuid(from.uuid())
      .setBChanged(false)
      .setBKey(from.getDbKey())
      .setBCopyComponentUuid(from.getCopyResourceUuid())
      .setBDescription(from.description())
      .setBEnabled(from.isEnabled())
      .setBUuidPath(from.getUuidPath())
      .setBLanguage(from.language())
      .setBLongName(from.longName())
      .setBModuleUuid(from.moduleUuid())
      .setBModuleUuidPath(from.moduleUuidPath())
      .setBName(from.name())
      .setBPath(from.path())
      // We don't have a b_scope. The applyBChangesForRootComponentUuid query is using a case ... when to infer scope from the qualifier
      .setBQualifier(from.qualifier());
  }
}
