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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileOperations implements ServerComponent {

  private static final String PROPERTY_PREFIX = "sonar.profile.";

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final PropertiesDao propertiesDao;
  private final List<ProfileImporter> importers;
  private final PreviewCache dryRunCache;
  private final RuleRegistry ruleRegistry;
  private final ProfileRules profileRules;
  private final ProfilesManager profilesManager;

  private final System2 system;

  /**
   * Used by pico when no plugin provide profile exporter / importer
   */
  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PropertiesDao propertiesDao,
                            PreviewCache dryRunCache, RuleRegistry ruleRegistry, ProfilesManager profilesManager, ProfileRules profileRules) {
    this(myBatis, dao, activeRuleDao, ruleDao, propertiesDao, Lists.<ProfileImporter>newArrayList(), dryRunCache, ruleRegistry,
      profilesManager, profileRules, System2.INSTANCE);
  }

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PropertiesDao propertiesDao,
                            List<ProfileImporter> importers, PreviewCache dryRunCache, RuleRegistry ruleRegistry,
                            ProfilesManager profilesManager, ProfileRules profileRules) {
    this(myBatis, dao, activeRuleDao, ruleDao, propertiesDao, Lists.<ProfileImporter>newArrayList(), dryRunCache, ruleRegistry,
      profilesManager, profileRules, System2.INSTANCE);
  }

  @VisibleForTesting
  QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PropertiesDao propertiesDao,
                            List<ProfileImporter> importers, PreviewCache dryRunCache, RuleRegistry ruleRegistry,
                            ProfilesManager profilesManager, ProfileRules profileRules, System2 system) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.propertiesDao = propertiesDao;
    this.importers = importers;
    this.dryRunCache = dryRunCache;
    this.ruleRegistry = ruleRegistry;
    this.profilesManager = profilesManager;
    this.profileRules = profileRules;
    this.system = system;
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    checkPermission(userSession);

    NewProfileResult result = new NewProfileResult();
    List<RulesProfile> importProfiles = readProfilesFromXml(result, xmlProfilesByPlugin);

    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language).setVersion(1).setUsed(false);
      dao.insert(dto, session);
      for (RulesProfile rulesProfile : importProfiles) {
        importProfile(dto, rulesProfile, session);
      }
      result.setProfile(QProfile.from(dto));
      session.commit();
      dryRunCache.reportGlobalModification();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return result;
  }

  public void renameProfile(QualityProfileDto qualityProfile, String newName, UserSession userSession) {
    checkPermission(userSession);
    qualityProfile.setName(newName);
    dao.update(qualityProfile);
  }

  public void setDefaultProfile(QualityProfileDto qualityProfile, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.setProperty(new PropertyDto().setKey(PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()));
  }

  public RuleActivationResult createActiveRule(QualityProfileDto qualityProfile, Rule rule, String severity, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = new ActiveRuleDto()
        .setProfileId(qualityProfile.getId())
        .setRuleId(rule.getId())
        .setSeverity(Severity.ordinal(severity));
      activeRuleDao.insert(activeRule, session);

      List<RuleParamDto> ruleParams = ruleDao.selectParameters(rule.getId(), session);
      List<ActiveRuleParamDto> activeRuleParams = Lists.newArrayList();
      for (RuleParamDto ruleParam : ruleParams) {
        ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
          .setActiveRuleId(activeRule.getId())
          .setRulesParameterId(ruleParam.getId())
          .setKey(ruleParam.getName())
          .setValue(ruleParam.getDefaultValue());
        activeRuleParams.add(activeRuleParam);
        activeRuleDao.insert(activeRuleParam, session);
      }
      session.commit();

      RuleInheritanceActions actions = profilesManager.activated(qualityProfile.getId(), activeRule.getId(), userSession.name());
      reindexInheritanceResult(actions, session);

      return new RuleActivationResult(QProfile.from(qualityProfile), profileRules.getFromActiveRuleId(activeRule.getId()));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void reindexInheritanceResult(RuleInheritanceActions actions, SqlSession session) {
    ruleRegistry.deleteActiveRules(actions.idsToDelete());
    List<ActiveRuleDto> activeRules = activeRuleDao.selectByIds(actions.idsToIndex(), session);
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    for(ActiveRuleParamDto param: activeRuleDao.selectParamsByRuleIds(actions.idsToIndex(), session)) {
      paramsByActiveRule.put(param.getActiveRuleId(), param);
    }
    ruleRegistry.bulkIndexActiveRules(activeRules, paramsByActiveRule);
  }

  public RuleActivationResult updateSeverity(QualityProfileDto qualityProfile, ActiveRuleDto activeRule, String newSeverity, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      Integer oldSeverity = activeRule.getSeverity();
      activeRule.setSeverity(Severity.ordinal(newSeverity));
      activeRuleDao.update(activeRule, session);
      session.commit();

      profilesManager.ruleSeverityChanged(activeRule.getProfileId(), activeRule.getId(), RulePriority.valueOfInt(oldSeverity), RulePriority.valueOf(newSeverity),
        userSession.name());
      return new RuleActivationResult(QProfile.from(qualityProfile), profileRules.getFromActiveRuleId(activeRule.getId()));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public RuleActivationResult deactivateRule(QualityProfileDto qualityProfile, Rule rule, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = validate(qualityProfile, rule);
      RuleInheritanceActions actions = profilesManager.deactivated(activeRule.getProfileId(), activeRule.getId(), userSession.name());

      activeRuleDao.delete(activeRule.getId(), session);
      activeRuleDao.deleteParameters(activeRule.getId(), session);
      actions.addToDelete(activeRule.getId());
      session.commit();

      reindexInheritanceResult(actions, session);

      return new RuleActivationResult(QProfile.from(qualityProfile), profileRules.getFromRuleId(rule.getId()));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void createActiveRuleParam(ActiveRuleDto activeRule, String key, String value, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      RuleParamDto ruleParam = ruleDao.selectParamByRuleAndKey(activeRule.getRulId(), key, session);
      if (ruleParam == null) {
        throw new IllegalArgumentException("No rule param found");
      }
      ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setActiveRuleId(activeRule.getId()).setKey(key).setValue(value).setRulesParameterId(ruleParam.getId());
      activeRuleDao.insert(activeRuleParam, session);
      session.commit();
      profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), key, null, value, userSession.name());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      activeRuleDao.deleteParameter(activeRuleParam.getId(), session);
      session.commit();
      profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), activeRuleParam.getKey(), activeRuleParam.getValue(), null, userSession.name());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, String value, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      String sanitizedValue = Strings.emptyToNull(value);
      String oldValue = activeRuleParam.getValue();
      activeRuleParam.setValue(sanitizedValue);
      activeRuleDao.update(activeRuleParam, session);
      session.commit();
      profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), activeRuleParam.getKey(), oldValue, sanitizedValue, userSession.name());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateActiveRuleNote(ActiveRuleDto activeRule, String note, UserSession userSession) {
    checkPermission(userSession);
    Date now = new Date(system.now());

    if (activeRule.getNoteData() == null) {
      activeRule.setNoteCreatedAt(now);
      activeRule.setNoteUserLogin(userSession.login());
    }
    activeRule.setNoteUpdatedAt(now);
    activeRule.setNoteData(note);
    activeRuleDao.update(activeRule);
    // TODO notify E/S of active rule change
  }

  public void deleteActiveRuleNote(ActiveRuleDto activeRule, UserSession userSession) {
    checkPermission(userSession);

    activeRule.setNoteUpdatedAt(new Date(system.now()));
    activeRule.setNoteData(null);
    activeRule.setNoteUserLogin(null);
    activeRule.setNoteCreatedAt(null);
    activeRule.setNoteUpdatedAt(null);
    activeRuleDao.update(activeRule);
    // TODO notify E/S of active rule change
  }

  private List<RulesProfile> readProfilesFromXml(NewProfileResult result, Map<String, String> xmlProfilesByPlugin) {
    List<RulesProfile> profiles = newArrayList();
    ValidationMessages messages = ValidationMessages.create();
    for (Map.Entry<String, String> entry : xmlProfilesByPlugin.entrySet()) {
      String pluginKey = entry.getKey();
      String file = entry.getValue();
      ProfileImporter importer = getProfileImporter(pluginKey);
      RulesProfile profile = importer.importProfile(new StringReader(file), messages);
      processValidationMessages(messages, result);
      profiles.add(profile);
    }
    return profiles;
  }

  private void importProfile(QualityProfileDto qualityProfileDto, RulesProfile rulesProfile, SqlSession sqlSession) {
    List<ActiveRuleDto> activeRuleDtos = newArrayList();
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    for (ActiveRule activeRule : rulesProfile.getActiveRules()) {
      ActiveRuleDto activeRuleDto = toActiveRuleDto(activeRule, qualityProfileDto);
      activeRuleDao.insert(activeRuleDto, sqlSession);
      activeRuleDtos.add(activeRuleDto);
      for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
        ActiveRuleParamDto activeRuleParamDto = toActiveRuleParamDto(activeRuleParam, activeRuleDto);
        activeRuleDao.insert(activeRuleParamDto, sqlSession);
        paramsByActiveRule.put(activeRuleDto.getId(), activeRuleParamDto);
      }
    }
    ruleRegistry.bulkIndexActiveRules(activeRuleDtos, paramsByActiveRule);
  }

  private ProfileImporter getProfileImporter(String exporterKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(exporterKey, importer.getKey())) {
        return importer;
      }
    }
    return null;
  }

  private void processValidationMessages(ValidationMessages messages, NewProfileResult result) {
    if (!messages.getErrors().isEmpty()) {
      List<BadRequestException.Message> errors = newArrayList();
      for (String error : messages.getErrors()) {
        errors.add(BadRequestException.Message.of(error));
      }
      throw BadRequestException.of("Fail to create profile", errors);
    }
    result.setWarnings(messages.getWarnings());
    result.setInfos(messages.getInfos());
  }

  private ActiveRuleDto toActiveRuleDto(ActiveRule activeRule, QualityProfileDto dto) {
    return new ActiveRuleDto()
      .setProfileId(dto.getId())
      .setRuleId(activeRule.getRule().getId())
      .setSeverity(toSeverityLevel(activeRule.getSeverity()));
  }

  private Integer toSeverityLevel(RulePriority rulePriority) {
    return rulePriority.ordinal();
  }

  private ActiveRuleParamDto toActiveRuleParamDto(ActiveRuleParam activeRuleParam, ActiveRuleDto activeRuleDto) {
    return new ActiveRuleParamDto()
      .setActiveRuleId(activeRuleDto.getId())
      .setRulesParameterId(activeRuleParam.getRuleParam().getId())
      .setKey(activeRuleParam.getKey())
      .setValue(activeRuleParam.getValue());
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private ActiveRuleDto validate(QualityProfileDto qualityProfile, Rule rule) {
    ActiveRuleDto activeRuleDto = findActiveRule(qualityProfile, rule);
    if (activeRuleDto == null) {
      throw new BadRequestException("No rule has been activated on this profile.");
    }
    return activeRuleDto;
  }

  @CheckForNull
  private ActiveRuleDto findActiveRule(QualityProfileDto qualityProfile, Rule rule) {
    return activeRuleDao.selectByProfileAndRule(qualityProfile.getId(), rule.getId());
  }

}
