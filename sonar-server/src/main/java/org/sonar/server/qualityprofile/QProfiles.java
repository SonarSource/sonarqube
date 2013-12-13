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

package org.sonar.server.qualityprofile;

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

public class QProfiles implements ServerComponent {

  private final QProfileSearch search;
  private final QProfileOperations operations;

  public QProfiles(QProfileSearch search, QProfileOperations operations) {
    this.search = search;
    this.operations = operations;
  }

  public List<QProfile> searchProfiles() {
    return search.searchProfiles();
  }

  public void searchProfile(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin) {
    return operations.newProfile(name, language, xmlProfilesByPlugin, UserSession.get());
  }

  public void deleteProfile() {
    throw new UnsupportedOperationException();
  }

  public void renameProfile() {
    throw new UnsupportedOperationException();
  }

  public void setDefaultProfile() {
    throw new UnsupportedOperationException();
  }

  public void copyProfile() {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(QProfileKey profile, String plugin) {
    throw new UnsupportedOperationException();
  }

  public void restoreProfile() {
    throw new UnsupportedOperationException();
  }

  // INHERITANCE

  public void inheritance() {
    throw new UnsupportedOperationException();
  }

  public void inherit(QProfileKey currentProfile, QProfileKey parentProfile) {
    throw new UnsupportedOperationException();
  }

  // CHANGELOG

  public void changelog(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  // PROJECTS

  public void projects(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void addProject(QProfileKey profile, String projectKey) {
    throw new UnsupportedOperationException();
  }

  public void removeProject(QProfileKey profile, String projectKey) {
    throw new UnsupportedOperationException();
  }

  public void removeAllProjects(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  // ACTIVE RULES

  public void searchActiveRules(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void searchInactiveRules(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void activeRule(QProfileKey profile, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void deactiveRule(QProfileKey profile, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  // TEMPLATE RULES

  public void createTemplateRule() {
    throw new UnsupportedOperationException();
  }

  public void editTemplateRule() {
    throw new UnsupportedOperationException();
  }

  public void deleteTemplateRule() {
    throw new UnsupportedOperationException();
  }

}
