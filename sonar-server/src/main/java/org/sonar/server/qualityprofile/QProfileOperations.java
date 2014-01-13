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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileOperations implements ServerComponent {

  public static final String PROFILE_PROPERTY_PREFIX = "sonar.profile.";

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final PropertiesDao propertiesDao;
  private final QProfileLookup profileLookup;
  private final List<ProfileImporter> importers;
  private final PreviewCache dryRunCache;
  private final RuleRegistry ruleRegistry;
  private final ProfilesManager profilesManager;

  /**
   * Used by pico when no plugin provide profile exporter / importer
   */
  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, PropertiesDao propertiesDao, QProfileLookup profileLookup,
                            PreviewCache dryRunCache, RuleRegistry ruleRegistry, ProfilesManager profilesManager) {
    this(myBatis, dao, activeRuleDao, propertiesDao, profileLookup, Lists.<ProfileImporter>newArrayList(), dryRunCache, ruleRegistry, profilesManager);
  }

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, PropertiesDao propertiesDao, QProfileLookup profileLookup,
                            List<ProfileImporter> importers, PreviewCache dryRunCache, RuleRegistry ruleRegistry, ProfilesManager profilesManager) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.propertiesDao = propertiesDao;
    this.importers = importers;
    this.dryRunCache = dryRunCache;
    this.ruleRegistry = ruleRegistry;
    this.profilesManager = profilesManager;
    this.profileLookup = profileLookup;
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    checkPermission(userSession);
    checkNotAlreadyExists(name, language);

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

  public void renameProfile(int profileId, String newName, UserSession userSession) {
    checkPermission(userSession);
    QProfile profile = profileLookup.profile(profileId);
    if (!profile.name().equals(newName)) {
      checkNotAlreadyExists(newName, profile.language());
    }
    QualityProfileDto dto = profile.toDto();
    dto.setName(newName);
    dao.update(dto);
  }

  public void setDefaultProfile(QualityProfileDto qualityProfile, UserSession userSession) {
    checkPermission(userSession);
    propertiesDao.setProperty(new PropertyDto().setKey(PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()));
  }

  public void updateParentProfile(int profileId, @Nullable Integer parentId, UserSession userSession) {
    checkPermission(userSession);
    QualityProfileDto profile = profileLookup.profile(profileId).toDto();
    QualityProfileDto parentProfile = null;
    if (parentId != null) {
      parentProfile = profileLookup.profile(parentId).toDto();
    }

    SqlSession session = myBatis.openSession();
    try {
      if (isCycle(profile, parentProfile, session)) {
        throw new BadRequestException("Please do not select a child profile as parent.");
      }
      String parentName = parentProfile != null ? parentProfile.getName() : null;

      ProfilesManager.RuleInheritanceActions actions = profilesManager.profileParentChanged(profile.getId(), parentName, userSession.name());
      ruleRegistry.deleteActiveRules(actions.idsToDelete());
      ruleRegistry.bulkIndexActiveRules(actions.idsToIndex(), session);

      profile.setParent(parentName);
      dao.update(profile, session);
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  boolean isCycle(QualityProfileDto childProfile, @Nullable QualityProfileDto parentProfile, SqlSession session) {
    QualityProfileDto currentParent = parentProfile;
    while (currentParent != null) {
      if (childProfile.getName().equals(currentParent.getName())) {
        return true;
      }
      currentParent = getParent(currentParent, session);
    }
    return false;
  }

  @CheckForNull
  private QualityProfileDto getParent(QualityProfileDto profile, SqlSession session) {
    if (profile.getParent() != null) {
      return dao.selectParent(profile.getId(), session);
    }
    return null;
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
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private void checkNotAlreadyExists(String name, String language) {
    if (dao.selectByNameAndLanguage(name, language) != null) {
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }
  }

  public static class NewProfileResult {

    private List<String> warnings;
    private List<String> infos;

    private QProfile profile;

    public NewProfileResult() {
      warnings = newArrayList();
      infos = newArrayList();
    }

    public List<String> warnings() {
      return warnings;
    }

    public NewProfileResult setWarnings(List<String> warnings) {
      this.warnings = warnings;
      return this;
    }

    public List<String> infos() {
      return infos;
    }

    public NewProfileResult setInfos(List<String> infos) {
      this.infos = infos;
      return this;
    }

    public QProfile profile() {
      return profile;
    }

    public NewProfileResult setProfile(QProfile profile) {
      this.profile = profile;
      return this;
    }
  }

}
