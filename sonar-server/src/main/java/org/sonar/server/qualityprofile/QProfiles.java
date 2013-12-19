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

import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.ProfileRuleQuery;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

public class QProfiles implements ServerComponent {

  private final QualityProfileDao qualityProfileDao;
  private final ResourceDao resourceDao;
  private final RuleFinder ruleFinder;

  private final QProfileProjectService projectService;

  private final QProfileSearch search;
  private final QProfileOperations operations;
  private final ProfileRules rules;

  public QProfiles(QualityProfileDao qualityProfileDao, ResourceDao resourceDao, RuleFinder ruleFinder, QProfileProjectService projectService, QProfileSearch search,
                   QProfileOperations operations, ProfileRules rules) {
    this.qualityProfileDao = qualityProfileDao;
    this.resourceDao = resourceDao;
    this.ruleFinder = ruleFinder;
    this.projectService = projectService;
    this.search = search;
    this.operations = operations;
    this.rules = rules;
  }

  // TODO
  //
  // PROFILES
  //
  // startup synchronisation
  // delete profile from profile id (Delete alerts, activeRules, activeRuleParams, activeRuleNotes, Projects)
  // copy profile
  // export profile from profile id
  // export profile from profile id and plugin key
  // restore profile
  //
  // INHERITANCE
  // get inheritance of profile id
  // change inheritance of a profile id
  //
  // CHANGELOG
  // display changelog for a profile id
  //
  // ACTIVE RULES
  // update parameter on a active rule
  // add note on an active rule
  // delete note on an active rule
  // edit note on an active rule
  // extends extension of a rule
  //
  // TEMPLATE RULES
  // create template rule
  // edit template rule
  // delete template rule

  public QProfile profile(int id) {
    QualityProfileDto dto = findNotNull(id);
    return QProfile.from(dto);
  }

  public List<QProfile> allProfiles() {
    return search.allProfiles();
  }

  @CheckForNull
  public QProfile defaultProfile(String language) {
    return search.defaultProfile(language);
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin) {
    validateNewProfile(name, language);
    return operations.newProfile(name, language, xmlProfilesByPlugin, UserSession.get());
  }

  public void renameProfile(int profileId, String newName) {
    QualityProfileDto qualityProfile = validateRenameProfile(profileId, newName);
    operations.renameProfile(qualityProfile, newName, UserSession.get());
  }

  public void setDefaultProfile(int profileId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    operations.setDefaultProfile(qualityProfile, UserSession.get());
  }

  /**
   * Used by WS
   */
  public void setDefaultProfile(String name, String language) {
    QualityProfileDto qualityProfile = findNotNull(name, language);
    operations.setDefaultProfile(qualityProfile, UserSession.get());
  }

  // PROJECTS

  public QProfileProjects projects(int profileId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    return projectService.projects(qualityProfile);
  }

  /**
   * Used in /project/profile
   */
  public List<QProfile> profiles(int projectId) {
    return projectService.profiles(projectId);
  }

  public void addProject(int profileId, long projectId) {
    ComponentDto project = (ComponentDto) findProjectNotNull(projectId);
    QualityProfileDto qualityProfile = findNotNull(profileId);

    projectService.addProject(qualityProfile, project, UserSession.get());
  }

  public void removeProject(int profileId, long projectId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    ComponentDto project = (ComponentDto) findProjectNotNull(projectId);

    projectService.removeProject(qualityProfile, project, UserSession.get());
  }

  public void removeProjectByLanguage(String language, long projectId) {
    Validation.checkMandatoryParameter(language, "language");
    ComponentDto project = (ComponentDto) findProjectNotNull(projectId);

    projectService.removeProject(language, project, UserSession.get());
  }

  public void removeAllProjects(int profileId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);

    projectService.removeAllProjects(qualityProfile, UserSession.get());
  }

  // ACTIVE RULES

  public QProfileRuleResult searchActiveRules(ProfileRuleQuery query, Paging paging) {
    return rules.searchActiveRules(query, paging);
  }

  public long countActiveRules(ProfileRuleQuery query) {
    return rules.countActiveRules(query);
  }

  public QProfileRuleResult searchInactiveRules(ProfileRuleQuery query, Paging paging) {
    return rules.searchInactiveRules(query, paging);
  }

  public long countInactiveRules(ProfileRuleQuery query) {
    return rules.countInactiveRules(query);
  }

  public RuleActivationResult activateRule(int profileId, int ruleId, String severity) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    Rule rule = findRuleNotNull(ruleId);
    return operations.activateRule(qualityProfile, rule, severity, UserSession.get());
  }

  public RuleActivationResult deactivateRule(int profileId, int ruleId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    Rule rule = findRuleNotNull(ruleId);
    return operations.deactivateRule(qualityProfile, rule, UserSession.get());
  }

  //
  // Quality profile validation
  //

  private void validateNewProfile(String name, String language) {
    validateProfileName(name);
    Validation.checkMandatoryParameter(language, "language");
    checkNotAlreadyExists(name, language);
  }

  private QualityProfileDto validateRenameProfile(Integer profileId, String newName) {
    validateProfileName(newName);
    QualityProfileDto profileDto = findNotNull(profileId);
    if (!profileDto.getName().equals(newName)) {
      // TODO move this check to the service
      checkNotAlreadyExists(newName, profileDto.getLanguage());
    }
    return profileDto;
  }

  private void checkNotAlreadyExists(String name, String language) {
    if (find(name, language) != null) {
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }
  }

  private QualityProfileDto findNotNull(int id) {
    QualityProfileDto qualityProfile = find(id);
    return checkNotNull(qualityProfile);
  }

  private QualityProfileDto findNotNull(String name, String language) {
    QualityProfileDto qualityProfile = find(name, language);
    return checkNotNull(qualityProfile);
  }

  private QualityProfileDto checkNotNull(QualityProfileDto qualityProfile) {
    if (qualityProfile == null) {
      throw new NotFoundException("This quality profile does not exists.");
    }
    return qualityProfile;
  }

  @CheckForNull
  private QualityProfileDto find(String name, String language) {
    return qualityProfileDao.selectByNameAndLanguage(name, language);
  }

  @CheckForNull
  private QualityProfileDto find(int id) {
    return qualityProfileDao.selectById(id);
  }

  private void validateProfileName(String name) {
    if (Strings.isNullOrEmpty(name)) {
      throw BadRequestException.ofL10n("quality_profiles.please_type_profile_name");
    }
  }

  //
  // Project validation
  //

  private Component findProjectNotNull(long projectId) {
    Component component = resourceDao.findById(projectId);
    if (component == null) {
      throw new NotFoundException("This project does not exists.");
    }
    return component;
  }

  //
  // Rule validation
  //

  private Rule findRuleNotNull(int ruleId) {
    Rule rule = ruleFinder.findById(ruleId);
    if (rule == null) {
      throw new NotFoundException("This rule does not exists.");
    }
    return rule;
  }


}
