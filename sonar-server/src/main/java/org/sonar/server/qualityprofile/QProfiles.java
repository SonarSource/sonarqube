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
import org.sonar.server.rule.ProfileRuleQuery;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

public class QProfiles implements ServerComponent {

  private final QProfileSearch search;
  private final QProfileOperations operations;
  private final ProfileRules rules;

  public QProfiles(QProfileSearch search, QProfileOperations operations, ProfileRules rules) {
    this.search = search;
    this.operations = operations;
    this.rules = rules;
  }

  public List<QProfile> searchProfiles() {
    throw new UnsupportedOperationException();
  }

  public void searchProfile(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin) {
    return operations.newProfile(name, language, xmlProfilesByPlugin, UserSession.get());
  }

  public void deleteProfile() {
    // Delete alerts, activeRules, activeRuleParams, activeRuleNotes, Projects
    throw new UnsupportedOperationException();
  }

  public void renameProfile(Integer profileId, String newName) {
    operations.renameProfile(profileId, newName, UserSession.get());
  }

  public void updateDefaultProfile(Integer profileId) {
    operations.updateDefaultProfile(profileId, UserSession.get());
  }

  /**
   * Used by WS
   */
  public void updateDefaultProfile(String name, String language) {
    operations.updateDefaultProfile(name, language, UserSession.get());
  }

  public void copyProfile() {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(Integer profileId, String plugin) {
    throw new UnsupportedOperationException();
  }

  public void restoreProfile() {
    throw new UnsupportedOperationException();
  }

  // INHERITANCE

  public void inheritance() {
    throw new UnsupportedOperationException();
  }

  public void inherit(Integer profileId, Integer parentProfileId) {
    throw new UnsupportedOperationException();
  }

  // CHANGELOG

  public void changelog(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  // PROJECTS

  public QProfileProjects projects(Integer profileId) {
    return operations.projects(profileId);
  }

  public void addProject(Integer profileId, Long projectId) {
    operations.addProject(profileId, projectId, UserSession.get());
  }

  public void removeProject(Integer profileId, String projectKey) {
    throw new UnsupportedOperationException();
  }

  public void removeAllProjects(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  // ACTIVE RULES

  public QProfileRuleResult searchActiveRules(ProfileRuleQuery query, Paging paging) {
    return rules.searchActiveRules(query, paging);
  }

  public long countActiveRules(ProfileRuleQuery query) {
    return rules.countActiveRules(query);
  }

  public void searchInactiveRules(ProfileRuleQuery query, Paging paging) {
    throw new UnsupportedOperationException();
  }

  public long countInactiveRules(ProfileRuleQuery query) {
    return rules.countInactiveRules(query);
  }

  public void activeRule(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void deactiveRule(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void updateParameters(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void activeNote(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void editNote(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void deleteNote(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void extendDescription(Integer profileId, RuleKey ruleKey) {
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
