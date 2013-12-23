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
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.ProfileRuleQuery;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Used through ruby code <pre>Internal.quality_profiles</pre>
 */
public class QProfiles implements ServerComponent {

  private final QualityProfileDao qualityProfileDao;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final ResourceDao resourceDao;

  private final QProfileProjectService projectService;

  private final QProfileSearch search;
  private final QProfileOperations operations;
  private final ProfileRules rules;

  public QProfiles(QualityProfileDao qualityProfileDao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, ResourceDao resourceDao,
                   QProfileProjectService projectService, QProfileSearch search, QProfileOperations operations, ProfileRules rules) {
    this.qualityProfileDao = qualityProfileDao;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.resourceDao = resourceDao;
    this.projectService = projectService;
    this.search = search;
    this.operations = operations;
    this.rules = rules;
  }

  // TODO
  //
  // PROFILES
  // list all profiles (including activated rules count)
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
  // ACTIVE RULES
  // bulk activate all
  // bulk deactivate all
  // update note on an active rule (only E/S indexing)
  // delete note on an active rule (only E/S indexing)
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

  public ActiveRuleChanged activateRule(int profileId, int ruleId, String severity) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    RuleDto rule = findRuleNotNull(ruleId);
    ActiveRuleDto activeRule = findActiveRule(qualityProfile, rule);
    if (activeRule == null) {
      activeRule = operations.createActiveRule(qualityProfile, rule, severity, UserSession.get());
    } else {
      operations.updateSeverity(qualityProfile, activeRule, severity, UserSession.get());
    }
    return activeRuleChanged(qualityProfile, activeRule);
  }

  public ActiveRuleChanged deactivateRule(int profileId, int ruleId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    RuleDto rule = findRuleNotNull(ruleId);
    ActiveRuleDto activeRule = findActiveRuleNotNull(qualityProfile, rule);
    operations.deactivateRule(activeRule, UserSession.get());
    return activeRuleChanged(qualityProfile, activeRule);
  }

  public ActiveRuleChanged updateActiveRuleParam(int profileId, int activeRuleId, String key, @Nullable String value) {
    String sanitizedValue = Strings.emptyToNull(value);
    QualityProfileDto qualityProfile = findNotNull(profileId);
    ActiveRuleParamDto activeRuleParam = findActiveRuleParam(activeRuleId, key);
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    UserSession userSession = UserSession.get();
    if (activeRuleParam == null && sanitizedValue != null) {
      operations.createActiveRuleParam(activeRule, key, value, userSession);
    } else if (activeRuleParam != null && sanitizedValue == null) {
      operations.deleteActiveRuleParam(activeRule, activeRuleParam, userSession);
    } else if (activeRuleParam != null) {
      operations.updateActiveRuleParam(activeRule, activeRuleParam, value, userSession);
    } else {
      // No active rule param and no value -> do nothing
    }
    return activeRuleChanged(qualityProfile, activeRule);
  }

  public QProfileRule updateActiveRuleNote(int activeRuleId, String note) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    String sanitizedNote = Strings.emptyToNull(note);
    if (sanitizedNote != null) {
      operations.updateActiveRuleNote(activeRule, note, UserSession.get());
    } else {
      // Empty note -> do nothing
    }
    return rules.getFromActiveRuleId(activeRule.getId());
  }

  public QProfileRule deleteActiveRuleNote(int activeRuleId) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    operations.deleteActiveRuleNote(activeRule, UserSession.get());
    return rules.getFromActiveRuleId(activeRule.getId());
  }

  public QProfileRule updateRuleNote(int activeRuleId, int ruleId, String note) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    RuleDto rule = findRuleNotNull(ruleId);
    String sanitizedNote = Strings.emptyToNull(note);
    if (sanitizedNote != null) {
      operations.updateRuleNote(activeRule, rule, note, UserSession.get());
    } else {
      operations.deleteRuleNote(activeRule, rule, UserSession.get());
    }
    return rules.getFromActiveRuleId(activeRule.getId());
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

  private RuleDto findRuleNotNull(int ruleId) {
    RuleDto rule = ruleDao.selectById(ruleId);
    if (rule == null) {
      throw new NotFoundException("This rule does not exists.");
    }
    return rule;
  }

  //
  // Active Rule validation
  //

  private ActiveRuleDto findActiveRuleNotNull(int activeRuleId) {
    ActiveRuleDto activeRule = activeRuleDao.selectById(activeRuleId);
    if (activeRule == null) {
      throw new NotFoundException("This active rule does not exists.");
    }
    return activeRule;
  }

  private ActiveRuleDto findActiveRuleNotNull(QualityProfileDto qualityProfile, RuleDto rule) {
    ActiveRuleDto activeRuleDto = findActiveRule(qualityProfile, rule);
    if (activeRuleDto == null) {
      throw new BadRequestException("No rule has been activated on this profile.");
    }
    return activeRuleDto;
  }

  @CheckForNull
  private ActiveRuleDto findActiveRule(QualityProfileDto qualityProfile, RuleDto rule) {
    return activeRuleDao.selectByProfileAndRule(qualityProfile.getId(), rule.getId());
  }

  @CheckForNull
  private ActiveRuleParamDto findActiveRuleParam(int activeRuleId, String key) {
    return activeRuleDao.selectParamByActiveRuleAndKey(activeRuleId, key);
  }

  private ActiveRuleChanged activeRuleChanged(QualityProfileDto qualityProfile, ActiveRuleDto activeRule){
    return new ActiveRuleChanged(QProfile.from(qualityProfile), rules.getFromActiveRuleId(activeRule.getId()));
  }

}
