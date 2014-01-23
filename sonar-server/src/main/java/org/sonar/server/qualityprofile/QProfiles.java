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

import org.sonar.server.rule.RuleOperations;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
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

  private static final String LANGUAGE_PARAM = "language";

  private final QualityProfileDao qualityProfileDao;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final ResourceDao resourceDao;

  private final QProfileProjectOperations projectOperations;
  private final QProfileProjectLookup projectLookup;
  private final QProfileBackup backup;
  private final QProfilePluginExporter exporter;
  private final QProfileLookup profileLookup;
  private final QProfileOperations operations;
  private final QProfileActiveRuleOperations activeRuleOperations;
  private final RuleOperations ruleOperations;
  private final QProfileRuleLookup rules;

  public QProfiles(QualityProfileDao qualityProfileDao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, ResourceDao resourceDao,
                   QProfileProjectOperations projectOperations, QProfileProjectLookup projectLookup, QProfileBackup backup, QProfilePluginExporter exporter,
                   QProfileLookup profileLookup, QProfileOperations operations, QProfileActiveRuleOperations activeRuleOperations, RuleOperations ruleOperations,
                   QProfileRuleLookup rules) {
    this.qualityProfileDao = qualityProfileDao;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.resourceDao = resourceDao;
    this.projectOperations = projectOperations;
    this.projectLookup = projectLookup;
    this.backup = backup;
    this.exporter = exporter;
    this.profileLookup = profileLookup;
    this.operations = operations;
    this.activeRuleOperations = activeRuleOperations;
    this.ruleOperations = ruleOperations;
    this.rules = rules;
  }

  public List<QProfile> allProfiles() {
    return profileLookup.allProfiles();
  }

  public List<QProfile> profilesByLanguage(String language) {
    return profileLookup.profiles(language);
  }

  @CheckForNull
  public QProfile profile(int id) {
    return profileLookup.profile(id);
  }

  @CheckForNull
  public QProfile profile(String name, String language) {
    validateProfileName(name);
    Validation.checkMandatoryParameter(language, LANGUAGE_PARAM);
    return profileLookup.profile(name, language);
  }

  @CheckForNull
  public QProfile defaultProfile(String language) {
    return profileLookup.defaultProfile(language);
  }

  public int countChildren(QProfile profile) {
    return profileLookup.countChildren(profile);
  }

  public QProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin) {
    validateProfileName(name);
    Validation.checkMandatoryParameter(language, LANGUAGE_PARAM);
    return operations.newProfile(name, language, xmlProfilesByPlugin, UserSession.get());
  }

  public void renameProfile(int profileId, String newName) {
    validateProfileName(newName);
    operations.renameProfile(profileId, newName, UserSession.get());
  }

  public void setDefaultProfile(int profileId) {
    operations.setDefaultProfile(profileId, UserSession.get());
  }

  public void copyProfile(int profileId, String copyProfileName) {
    validateProfileName(copyProfileName);
    operations.copyProfile(profileId, copyProfileName, UserSession.get());
  }

  @CheckForNull
  public QProfile parent(QProfile profile) {
    return profileLookup.parent(profile);
  }

  public List<QProfile> children(QProfile profile) {
    return profileLookup.children(profile);
  }

  public List<QProfile> ancestors(QProfile profile) {
    return profileLookup.ancestors(profile);
  }

  public void updateParentProfile(int profileId, @Nullable Integer parentId) {
    operations.updateParentProfile(profileId, parentId, UserSession.get());
  }

  public QProfileResult restore(String xmlBackup, boolean deleteExisting) {
    return backup.restore(xmlBackup, deleteExisting, UserSession.get());
  }

  public String backupProfile(QProfile profile) {
    return backup.backupProfile(profile);
  }

  public void deleteProfile(int profileId) {
    operations.deleteProfile(profileId, UserSession.get());
  }

  public String exportProfileToXml(QProfile profile, String pluginKey) {
    return exporter.exportToXml(profile, pluginKey);
  }

  public String getProfileExporterMimeType(String exporterKey) {
    return exporter.getProfileExporterMimeType(exporterKey);
  }

  public List<ProfileExporter> getProfileExportersForLanguage(String language) {
    return exporter.getProfileExportersForLanguage(language);
  }

  public List<ProfileImporter> getProfileImportersForLanguage(String language) {
    return exporter.getProfileImportersForLanguage(language);
  }


  // PROJECTS

  public QProfileProjectLookup.QProfileProjects projects(int profileId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    return projectLookup.projects(qualityProfile);
  }

  public int countProjects(QProfile profile) {
    return projectLookup.countProjects(profile);
  }

  /**
   * Used in /project/profile
   */
  public List<QProfile> profiles(int projectId) {
    return projectLookup.profiles(projectId);
  }

  public void addProject(int profileId, long projectId) {
    ComponentDto project = (ComponentDto) findProjectNotNull(projectId);
    QualityProfileDto qualityProfile = findNotNull(profileId);

    projectOperations.addProject(qualityProfile, project, UserSession.get());
  }

  public void removeProject(int profileId, long projectId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    ComponentDto project = (ComponentDto) findProjectNotNull(projectId);

    projectOperations.removeProject(qualityProfile, project, UserSession.get());
  }

  public void removeProjectByLanguage(String language, long projectId) {
    Validation.checkMandatoryParameter(language, LANGUAGE_PARAM);
    ComponentDto project = (ComponentDto) findProjectNotNull(projectId);

    projectOperations.removeProject(language, project, UserSession.get());
  }

  public void removeAllProjects(int profileId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);

    projectOperations.removeAllProjects(qualityProfile, UserSession.get());
  }


  // PROFILE RULES

  public QProfileRule findByRule(int ruleId) {
    return rules.findByRuleId(ruleId);
  }

  public QProfileRule findByActiveRuleId(int activeRuleId) {
    return rules.findByActiveRuleId(activeRuleId);
  }

  public QProfileRule findByProfileAndRule(int profileId, int ruleId) {
    return rules.findByProfileIdAndRuleId(profileId, ruleId);
  }

  public QProfileRuleLookup.QProfileRuleResult searchProfileRules(ProfileRuleQuery query, Paging paging) {
    return rules.search(query, paging);
  }

  public long countProfileRules(ProfileRuleQuery query) {
    return rules.countProfileRules(query);
  }

  public QProfileRuleLookup.QProfileRuleResult searchInactiveProfileRules(ProfileRuleQuery query, Paging paging) {
    return rules.searchInactives(query, paging);
  }

  public long countInactiveProfileRules(ProfileRuleQuery query) {
    return rules.countInactiveProfileRules(query);
  }

  public long countProfileRules(QProfile profile) {
    return rules.countProfileRules(ProfileRuleQuery.create(profile.id()));
  }

  public long countOverridingProfileRules(QProfile profile) {
    return rules.countProfileRules(ProfileRuleQuery.create(profile.id()).setInheritance(QProfileRule.OVERRIDES));
  }

  public void activateRule(int profileId, int ruleId, String severity) {
    activeRuleOperations.activateRule(profileId, ruleId, severity, UserSession.get());
  }

  public int bulkActivateRule(ProfileRuleQuery query) {
    List<Integer> ruleIdsToActivate = rules.searchInactiveProfileRuleIds(query);
    activeRuleOperations.activateRules(query.profileId(), ruleIdsToActivate, UserSession.get());
    return ruleIdsToActivate.size();
  }

  public void deactivateRule(int profileId, int ruleId) {
    activeRuleOperations.deactivateRule(profileId, ruleId, UserSession.get());
  }

  public int bulkDeactivateRule(ProfileRuleQuery query) {
    List<Integer> activeRuleIdsToDeactivate = rules.searchProfileRuleIds(query);
    return activeRuleOperations.deactivateRules(activeRuleIdsToDeactivate, UserSession.get());
  }

  public void updateActiveRuleParam(int activeRuleId, String key, @Nullable String value) {
    activeRuleOperations.updateActiveRuleParam(activeRuleId, key, value, UserSession.get());
  }

  public void revertActiveRule(int activeRuleId) {
    activeRuleOperations.revertActiveRule(activeRuleId, UserSession.get());
  }

  public QProfileRule updateActiveRuleNote(int activeRuleId, String note) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    String sanitizedNote = Strings.emptyToNull(note);
    if (sanitizedNote != null) {
      activeRuleOperations.updateActiveRuleNote(activeRule, note, UserSession.get());
    }
    // Empty note -> do nothing

    return rules.findByActiveRuleId(activeRule.getId());
  }

  public QProfileRule deleteActiveRuleNote(int activeRuleId) {
    ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId);
    activeRuleOperations.deleteActiveRuleNote(activeRule, UserSession.get());
    return rules.findByActiveRuleId(activeRule.getId());
  }

  @CheckForNull
  public QProfileRule parentProfileRule(QProfileRule rule) {
    Integer parentId = rule.activeRuleParentId();
    if (parentId != null) {
      return rules.findByActiveRuleId(parentId);
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
    return rules.findByActiveRuleId(activeRule.getId());
  }

  @CheckForNull
  public QProfileRule rule(int ruleId) {
    return rules.findByRuleId(ruleId);
  }

  public QProfileRule createRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRule(null, name, severity, description);
    RuleDto newRule = ruleOperations.createRule(rule, name, severity, description, paramsByKey, UserSession.get());
    return rules.findByRuleId(newRule.getId());
  }

  public QProfileRule updateRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRuleParent(rule);
    validateRule(ruleId, name, severity, description);
    ruleOperations.updateRule(rule, name, severity, description, paramsByKey, UserSession.get());
    return rules.findByRuleId(ruleId);
  }

  public void deleteRule(int ruleId) {
    RuleDto rule = findRuleNotNull(ruleId);
    validateRuleParent(rule);
    ruleOperations.deleteRule(rule, UserSession.get());
  }

  public int countActiveRules(QProfileRule rule) {
    // TODO get it from E/S
    return activeRuleDao.selectByRuleId(rule.id()).size();
  }

  public QProfileRule updateRuleTags(int ruleId, Object tags) {
    RuleDto rule = findRuleNotNull(ruleId);
    List<String> newTags = RubyUtils.toStrings(tags);
    if (newTags == null) {
      newTags = ImmutableList.of();
    }
    ruleOperations.updateTags(rule, newTags, UserSession.get());
    return rules.findByRuleId(ruleId);
  }

  //
  // Quality profile validation
  //

  private QualityProfileDto findNotNull(int id) {
    QualityProfileDto qualityProfile = findQualityProfile(id);
    QProfileValidations.checkProfileIsNotNull(qualityProfile);
    return qualityProfile;
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
    QProfileValidations.checkRuleIsNotNull(rule);
    return rule;
  }

  private void validateRuleParent(RuleDto rule) {
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

}
