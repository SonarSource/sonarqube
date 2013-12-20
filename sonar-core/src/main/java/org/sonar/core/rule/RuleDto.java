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
package org.sonar.core.rule;

import org.sonar.check.Cardinality;
import org.sonar.check.Priority;

import java.util.Date;

public final class RuleDto {
  private Integer id;
  private String repositoryKey;
  private String ruleKey;
  private String description;
  private String status;
  private String name;
  private String configKey;
  private Priority priority;
  private Cardinality cardinality;
  private String language;
  private Integer parentId;
  private Date createdAt;
  private Date updatedAt;
  private String noteData;
  private String noteUserLogin;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public void setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getConfigKey() {
    return configKey;
  }

  public void setConfigKey(String configKey) {
    this.configKey = configKey;
  }

  public Priority getPriority() {
    return priority;
  }

  public int getPriorityOrdinal() {
    return priority.ordinal();
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public void setPriorityOrdinal(int ordinal) {
    this.priority = Priority.values()[ordinal];
  }

  public Cardinality getCardinality() {
    return cardinality;
  }

  public void setCardinality(Cardinality cardinality) {
    this.cardinality = cardinality;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Integer getParentId() {
    return parentId;
  }

  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getNoteData() {
    return noteData;
  }

  public void setNoteData(String noteData) {
    this.noteData = noteData;
  }

  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public void setNoteUserLogin(String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
  }

  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public void setNoteCreatedAt(Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
  }

  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public void setNoteUpdatedAt(Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
  }
}
