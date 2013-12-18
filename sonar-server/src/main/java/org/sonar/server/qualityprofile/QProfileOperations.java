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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
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
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.io.StringReader;
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
  private final List<ProfileExporter> exporters;
  private final List<ProfileImporter> importers;
  private final PreviewCache dryRunCache;
  private final RuleRegistry ruleRegistry;

  // Should not be used as it still uses Hibernate
  private final ProfilesManager profilesManager;

  /**
   * Used by pico when no plugin provide profile exporter / importer
   */
  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PropertiesDao propertiesDao,
                            PreviewCache dryRunCache, RuleRegistry ruleRegistry, ProfilesManager profilesManager) {
    this(myBatis, dao, activeRuleDao, ruleDao, propertiesDao, Lists.<ProfileExporter>newArrayList(), Lists.<ProfileImporter>newArrayList(), dryRunCache, ruleRegistry,
      profilesManager);
  }

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PropertiesDao propertiesDao,
                            List<ProfileExporter> exporters, List<ProfileImporter> importers, PreviewCache dryRunCache, RuleRegistry ruleRegistry,
                            ProfilesManager profilesManager) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.propertiesDao = propertiesDao;
    this.exporters = exporters;
    this.importers = importers;
    this.dryRunCache = dryRunCache;
    this.ruleRegistry = ruleRegistry;
    this.profilesManager = profilesManager;
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

  public void activateRule(QualityProfileDto qualityProfile, Rule rule, String severity, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = findActiveRule(qualityProfile, rule);
      if (activeRule == null) {
        newActiveRule(qualityProfile, rule, severity, userSession, session);
      } else {
        updateSeverity(activeRule, severity, userSession, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void newActiveRule(QualityProfileDto qualityProfile, Rule rule, String severity, UserSession userSession, SqlSession session) {
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileId(qualityProfile.getId())
      .setRuleId(rule.getId())
      .setSeverity(Severity.ordinal(severity));
    activeRuleDao.insert(activeRuleDto, session);

    List<RuleParamDto> ruleParams = ruleDao.selectParameters(rule.getId().longValue(), session);
    for (RuleParamDto ruleParam : ruleParams) {
      ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
        .setActiveRuleId(activeRuleDto.getId())
        .setRulesParameterId(ruleParam.getId())
        .setKey(ruleParam.getName())
        .setValue(ruleParam.getDefaultValue());
      activeRuleDao.insert(activeRuleParam, session);
    }
    session.commit();

    profilesManager.activated(qualityProfile.getId(), activeRuleDto.getId(), userSession.name());
  }

  private void updateSeverity(ActiveRuleDto activeRule, String newSeverity, UserSession userSession, SqlSession session) {
    Integer oldSeverity = activeRule.getSeverity();
    activeRule.setSeverity(Severity.ordinal(newSeverity));
    activeRuleDao.update(activeRule, session);
    session.commit();

    profilesManager.ruleSeverityChanged(activeRule.getProfileId(), activeRule.getId(), RulePriority.valueOfInt(oldSeverity), RulePriority.valueOf(newSeverity),
      userSession.name());
  }

  public void deactivateRule(QualityProfileDto qualityProfile, Rule rule, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = validate(qualityProfile, rule);

      activeRuleDao.delete(activeRule.getId(), session);
      activeRuleDao.deleteParameters(activeRule.getId(), session);
      session.commit();
      profilesManager.deactivated(activeRule.getProfileId(), activeRule.getId(), userSession.name());
    } finally {
      MyBatis.closeQuietly(session);
    }
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
