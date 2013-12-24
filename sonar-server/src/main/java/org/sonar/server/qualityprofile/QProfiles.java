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

import static com.google.common.collect.Lists.newArrayList;

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
  private final QProfileActiveRuleOperations activeRuleOperations;
  private final QProfileRuleOperations ruleOperations;
  private final ProfileRules rules;

  public QProfiles(QualityProfileDao qualityProfileDao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, ResourceDao resourceDao,
                   QProfileProjectService projectService, QProfileSearch search,
                   QProfileOperations operations, QProfileActiveRuleOperations activeRuleOperations, QProfileRuleOperations ruleOperations, ProfileRules rules) {
    this.qualityProfileDao = qualityProfileDao;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.resourceDao = resourceDao;
    this.projectService = projectService;
    this.search = search;
    this.operations = operations;
    this.activeRuleOperations = activeRuleOperations;
    this.ruleOperations = ruleOperations;
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
  // active rule parameter validation (only Integer types are checked)


  public QProfile profile(int id) {
    return QProfile.from(findNotNull(id));
  }

  @CheckForNull
  public QProfile parent(QProfile profile) {
    QualityProfileDto parent = findQualityProfile(profile.parent(), profile.language());
    if (parent != null) {
      return QProfile.from(parent);
    }
    return null;
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
      activeRule = activeRuleOperations.createActiveRule(qualityProfile, rule, severity, UserSession.get());
    } else {
      activeRuleOperations.updateSeverity(activeRule, severity, UserSession.get());
    }
    return activeRuleChanged(qualityProfile, activeRule);
  }

  public ActiveRuleChanged deactivateRule(int profileId, int ruleId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    RuleDto rule = findRuleNotNull(ruleId);
    ActiveRuleDto activeRule = findActiveRuleNotNull(qualityProfile, rule);
    activeRuleOperations.deactivateRule(activeRule, UserSession.get());
    return activeRuleChanged(qualityProfile, rule);
  }

  public ActiveRuleChanged updateActiveRuleParam(int profileId, int activeRuleId, String key, @Nullable String value) {
    String sanitizedValue = Strings.emptyToNull(value);
    QualityProfileDto qualityProfile = findNotNull(profileId);
    ActiveRuleParamDto activeRuleParam = findActiveRuleParam(activeRuleId, key);
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    UserSession userSession = UserSession.get();
    if (activeRuleParam == null && sanitizedValue != null) {
      activeRuleOperations.createActiveRuleParam(activeRule, key, value, userSession);
    } else if (activeRuleParam != null && sanitizedValue == null) {
      activeRuleOperations.deleteActiveRuleParam(activeRule, activeRuleParam, userSession);
    } else if (activeRuleParam != null) {
      activeRuleOperations.updateActiveRuleParam(activeRule, activeRuleParam, value, userSession);
    } else {
      // No active rule param and no value -> do nothing
    }
    return activeRuleChanged(qualityProfile, activeRule);
  }

  public ActiveRuleChanged revertActiveRule(int profileId, int activeRuleId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    activeRuleOperations.revertActiveRule(activeRule, UserSession.get());
    return activeRuleChanged(qualityProfile, activeRule);
  }

  public QProfileRule updateActiveRuleNote(int activeRuleId, String note) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    String sanitizedNote = Strings.emptyToNull(note);
    if (sanitizedNote != null) {
      activeRuleOperations.updateActiveRuleNote(activeRule, note, UserSession.get());
    } else {
      // Empty note -> do nothing
    }
    return rules.getFromActiveRuleId(activeRule.getId());
  }

  public QProfileRule deleteActiveRuleNote(int activeRuleId) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    activeRuleOperations.deleteActiveRuleNote(activeRule, UserSession.get());
    return rules.getFromActiveRuleId(activeRule.getId());
  }

  /**
   * Used to load ancestor param of an active rule param
   * <p/>
   * TODO Ancestor params should be integrated into QProfileRuleParam or elsewhere in order to load all ancestor params once a time
   */
  @CheckForNull
  public ActiveRuleParamDto activeRuleParam(QProfileRule rule, String key) {
    Integer activeRuleId = rule.activeRuleId();
    if (activeRuleId == null) {
      throw new IllegalArgumentException("Active rule id can't be null");
    }
    return findActiveRuleParam(activeRuleId, key);

  }

  /**
   * Used to load ancestor active rule of an active rule
   * <p/>
   * TODO Ancestor active rules should be integrated into QProfileRule or elsewhere in order to load all ancestor active rules once a time
   */
  @CheckForNull
  public QProfileRule parentActiveRule(QProfileRule rule) {
    ActiveRuleDto parent = activeRuleDao.selectParent(rule.activeRuleId());
    if (parent != null) {
      return rules.getFromActiveRuleId(parent.getId());
    }
    return null;
  }


  // RULES

  public QProfileRule updateRuleNote(int activeRuleId, int ruleId, String note) {
    RuleDto rule = findRuleNotNull(ruleId);
    String sanitizedNote = Strings.emptyToNull(note);
    if (sanitizedNote != null) {
      ruleOperations.updateRuleNote(rule, note, UserSession.get());
    } else {
      ruleOperations.deleteRuleNote(rule, UserSession.get());
    }
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    return rules.getFromActiveRuleId(activeRule.getId());
  }

  @CheckForNull
  public QProfileRule rule(int ruleId) {
    return rules.getFromRuleId(ruleId);
  }

  public QProfileRule createRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRule(null, name, severity, description);
    RuleDto newRule = ruleOperations.createRule(rule, name, severity, description, paramsByKey, UserSession.get());
    return rules.getFromRuleId(newRule.getId());
  }

  public QProfileRule updateRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRuleParent(rule);
    validateRule(ruleId, name, severity, description);
    ruleOperations.updateRule(rule, name, severity, description, paramsByKey, UserSession.get());
    return rules.getFromRuleId(ruleId);
  }

  public void deleteRule(int ruleId) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRuleParent(rule);
    ruleOperations.deleteRule(rule, UserSession.get());
  }

  public int countActiveRules(QProfileRule rule){
    return activeRuleDao.selectByRuleId(rule.id()).size();
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
      checkNotAlreadyExists(newName, profileDto.getLanguage());
    }
    return profileDto;
  }

  private void checkNotAlreadyExists(String name, String language) {
    if (findQualityProfile(name, language) != null) {
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }
  }

  private QualityProfileDto findNotNull(int id) {
    QualityProfileDto qualityProfile = findQualityProfile(id);
    return checkNotNull(qualityProfile);
  }

  private QualityProfileDto findNotNull(String name, String language) {
    QualityProfileDto qualityProfile = findQualityProfile(name, language);
    return checkNotNull(qualityProfile);
  }

  private QualityProfileDto checkNotNull(QualityProfileDto qualityProfile) {
    if (qualityProfile == null) {
      throw new NotFoundException("This quality profile does not exists.");
    }
    return qualityProfile;
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(String name, String language) {
    return qualityProfileDao.selectByNameAndLanguage(name, language);
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(int id) {
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

  private void validateRule(@Nullable Integer updatingRuleId, @Nullable String name, @Nullable String severity, @Nullable String description) {
    List<BadRequestException.Message> messages = newArrayList();
    if (Strings.isNullOrEmpty(name)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      checkRuleNotAlreadyExists(updatingRuleId, name, messages);
    }
    if (Strings.isNullOrEmpty(description)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Description"));
    }
    if (Strings.isNullOrEmpty(severity)) {
      messages.add(BadRequestException.Message.ofL10n(Validation.CANT_BE_EMPTY_MESSAGE, "Severity"));
    }
    if (!messages.isEmpty()) {
      throw new BadRequestException(null, messages);
    }
  }

  private void checkRuleNotAlreadyExists(@Nullable Integer updatingRuleId, String name, List<BadRequestException.Message> messages) {
    RuleDto existingRule = ruleDao.selectByName(name);
    boolean isModifyingCurrentRule = updatingRuleId != null && existingRule != null && existingRule.getId().equals(updatingRuleId);
    if (!isModifyingCurrentRule && existingRule != null) {
      messages.add(BadRequestException.Message.ofL10n(Validation.IS_ALREADY_USED_MESSAGE, "Name"));
    }
  }

  private RuleDto findRuleNotNull(int ruleId) {
    RuleDto rule = ruleDao.selectById(ruleId);
    if (rule == null) {
      throw new NotFoundException("This rule does not exists.");
    }
    return rule;
  }

  private void validateRuleParent(RuleDto rule){
    if (rule.getParentId() == null) {
      throw new NotFoundException("Unknown rule");
    }
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

  private ActiveRuleChanged activeRuleChanged(QualityProfileDto qualityProfile, ActiveRuleDto activeRule) {
    return new ActiveRuleChanged(QProfile.from(qualityProfile), rules.getFromActiveRuleId(activeRule.getId()));
  }

  private ActiveRuleChanged activeRuleChanged(QualityProfileDto qualityProfile, RuleDto rule) {
    return new ActiveRuleChanged(QProfile.from(qualityProfile), rules.getFromRuleId(rule.getId()));
  }

}
