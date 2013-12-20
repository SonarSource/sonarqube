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

import java.util.Date;

public class ActiveRuleDto {

  private Integer id;
  private Integer profileId;
  private Long ruleId;
  private Integer severity;
  private String inheritance;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;
  private String noteUserLogin;
  private String noteData;

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

  public Long getRulId() {
    return ruleId;
  }

  public ActiveRuleDto setRuleId(Long ruleId) {
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

  public String getInheritance() {
    return inheritance;
  }

  public ActiveRuleDto setInheritance(String inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public ActiveRuleDto setNoteCreatedAt(Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public ActiveRuleDto setNoteUpdatedAt(Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public ActiveRuleDto setNoteUserLogin(String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
    return this;
  }

  public String getNoteData() {
    return noteData;
  }

  public ActiveRuleDto setNoteData(String noteData) {
    this.noteData = noteData;
    return this;
  }
}
