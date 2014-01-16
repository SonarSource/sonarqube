/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.qualityprofile.db;

import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.persistence.Transient;

import java.util.Date;

public class ActiveRuleDto {

  public static final String INHERITED = "INHERITED";
  public static final String OVERRIDES = "OVERRIDES";

  private Integer id;
  private Integer profileId;
  private Integer ruleId;
  private Integer severity;
  private String inheritance;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;
  private String noteUserLogin;
  private String noteData;

  // This field do not exists in db, it's only retrieve by joins
  @Transient
  private Integer parentId;

  public Integer getId() {
    return id;
  }

  public ActiveRuleDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getProfileId() {
    return profileId;
  }

  public ActiveRuleDto setProfileId(Integer profileId) {
    this.profileId = profileId;
    return this;
  }

  public Integer getRulId() {
    return ruleId;
  }

  public ActiveRuleDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public Integer getSeverity() {
    return severity;
  }

  public ActiveRuleDto setSeverity(Integer severity) {
    this.severity = severity;
    return this;
  }

  @CheckForNull
  public String getInheritance() {
    return inheritance;
  }

  public ActiveRuleDto setInheritance(@Nullable String inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  @CheckForNull
  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public ActiveRuleDto setNoteCreatedAt(@Nullable Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  @CheckForNull
  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public ActiveRuleDto setNoteUpdatedAt(@Nullable Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  @CheckForNull
  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public ActiveRuleDto setNoteUserLogin(@Nullable String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
    return this;
  }

  @CheckForNull
  public String getNoteData() {
    return noteData;
  }

  public ActiveRuleDto setNoteData(@Nullable String noteData) {
    this.noteData = noteData;
    return this;
  }


  @CheckForNull
  public Integer getParentId() {
    return parentId;
  }

  public ActiveRuleDto setParentId(@Nullable Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  public boolean isInherited() {
    return StringUtils.equals(INHERITED, inheritance);
  }

  public boolean doesOverride() {
    return StringUtils.equals(OVERRIDES, inheritance);
  }


}
