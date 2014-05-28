/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.rule;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;

import java.util.Set;

class NewRule {
  private final RuleKey key;
  private boolean template;
  private Set<String> tags, systemTags;
  private String language, name, htmlDescription, severity;
  private RuleStatus status;
  private String internalKey;
  private String defaultDebtCharacteristic, defaultDebtSubCharacteristic, debtCharacteristic, debtSubCharacteristic;
  private DebtRemediationFunction defaultDebtRemediationFunction, debtRemediationFunction;
  private String markdownNote, noteLogin;

  NewRule(RuleKey key) {
    this.key = key;
  }

  public RuleKey getKey() {
    return key;
  }

  public boolean isTemplate() {
    return template;
  }

  public void setTemplate(boolean template) {
    this.template = template;
  }

  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  public Set<String> getSystemTags() {
    return systemTags;
  }

  public void setSystemTags(Set<String> systemTags) {
    this.systemTags = systemTags;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHtmlDescription() {
    return htmlDescription;
  }

  public void setHtmlDescription(String htmlDescription) {
    this.htmlDescription = htmlDescription;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public RuleStatus getStatus() {
    return status;
  }

  public void setStatus(RuleStatus status) {
    this.status = status;
  }

  public String getInternalKey() {
    return internalKey;
  }

  public void setInternalKey(String internalKey) {
    this.internalKey = internalKey;
  }

  public String getDefaultDebtCharacteristic() {
    return defaultDebtCharacteristic;
  }

  public void setDefaultDebtCharacteristic(String defaultDebtCharacteristic) {
    this.defaultDebtCharacteristic = defaultDebtCharacteristic;
  }

  public String getDefaultDebtSubCharacteristic() {
    return defaultDebtSubCharacteristic;
  }

  public void setDefaultDebtSubCharacteristic(String defaultDebtSubCharacteristic) {
    this.defaultDebtSubCharacteristic = defaultDebtSubCharacteristic;
  }

  public String getDebtCharacteristic() {
    return debtCharacteristic;
  }

  public void setDebtCharacteristic(String debtCharacteristic) {
    this.debtCharacteristic = debtCharacteristic;
  }

  public String getDebtSubCharacteristic() {
    return debtSubCharacteristic;
  }

  public void setDebtSubCharacteristic(String debtSubCharacteristic) {
    this.debtSubCharacteristic = debtSubCharacteristic;
  }

  public DebtRemediationFunction getDefaultDebtRemediationFunction() {
    return defaultDebtRemediationFunction;
  }

  public void setDefaultDebtRemediationFunction(DebtRemediationFunction defaultDebtRemediationFunction) {
    this.defaultDebtRemediationFunction = defaultDebtRemediationFunction;
  }

  public DebtRemediationFunction getDebtRemediationFunction() {
    return debtRemediationFunction;
  }

  public void setDebtRemediationFunction(DebtRemediationFunction debtRemediationFunction) {
    this.debtRemediationFunction = debtRemediationFunction;
  }

  public String getMarkdownNote() {
    return markdownNote;
  }

  public void setMarkdownNote(String markdownNote) {
    this.markdownNote = markdownNote;
  }

  public String getNoteLogin() {
    return noteLogin;
  }

  public void setNoteLogin(String noteLogin) {
    this.noteLogin = noteLogin;
  }
}
