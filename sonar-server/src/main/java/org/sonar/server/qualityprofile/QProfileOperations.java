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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileOperations implements ServerComponent {

  private static final String PROPERTY_PREFIX = "sonar.profile.";
  private static final String QUALITY_PROFILES_ALREADY_EXISTS = "quality_profiles.already_exists";

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final List<ProfileExporter> exporters;
  private final List<ProfileImporter> importers;
  private final PreviewCache dryRunCache;
  private final PersistentSettings persistentSettings;

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, PreviewCache dryRunCache, PersistentSettings persistentSettings) {
    this(myBatis, dao, activeRuleDao, Lists.<ProfileExporter>newArrayList(), Lists.<ProfileImporter>newArrayList(), dryRunCache, persistentSettings);
  }

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, List<ProfileExporter> exporters, List<ProfileImporter> importers,
                            PreviewCache dryRunCache, PersistentSettings persistentSettings) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.exporters = exporters;
    this.importers = importers;
    this.dryRunCache = dryRunCache;
    this.persistentSettings = persistentSettings;
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    validateNewProfile(name, language, userSession);

    NewProfileResult result = new NewProfileResult();
    List<RulesProfile> importProfiles = readProfilesFromXml(result, xmlProfilesByPlugin);

    SqlSession sqlSession = myBatis.openSession();
    try {
      QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language).setVersion(1).setUsed(false);
      dao.insert(dto, sqlSession);
      for (RulesProfile rulesProfile : importProfiles) {
        importProfile(dto, rulesProfile, sqlSession);
      }
      result.setProfile(QProfile.from(dto));
      sqlSession.commit();
      dryRunCache.reportGlobalModification();
    } finally {
      MyBatis.closeQuietly(sqlSession);
    }
    return result;
  }

  public void renameProfile(Integer profileId, String newName, UserSession userSession) {
    QualityProfileDto qualityProfile = validateRenameProfile(profileId, newName, userSession);
    qualityProfile.setName(newName);
    dao.update(qualityProfile);
  }

  public void updateDefaultProfile(Integer id, UserSession userSession) {
    QualityProfileDto qualityProfile = validateUpdateDefaultProfile(id, userSession);
    persistentSettings.saveProperty(PROPERTY_PREFIX + qualityProfile.getLanguage(), qualityProfile.getName());
  }

  public void updateDefaultProfile(String name, String language, UserSession userSession) {
    updateDefaultProfile(findNeverNull(name, language).getId(), userSession);
  }

  public QProfileProjects projects(Integer profileId) {
    QualityProfileDto dto = findNeverNull(profileId);
    List<ComponentDto> componentDtos = dao.selectProjects(PROPERTY_PREFIX + dto.getLanguage(), dto.getName());
    List<Component> projects = newArrayList(Iterables.transform(componentDtos, new Function<ComponentDto, Component>() {
      @Override
      public Component apply(@Nullable ComponentDto dto) {
        return (Component) dto;
      }
    }));
    return new QProfileProjects(QProfile.from(dto), projects);
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
    for (ActiveRule activeRule : rulesProfile.getActiveRules()) {
      ActiveRuleDto activeRuleDto = toActiveRuleDto(activeRule, qualityProfileDto);
      activeRuleDao.insert(activeRuleDto, sqlSession);
      for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
        activeRuleDao.insert(toActiveRuleParamDto(activeRuleParam, activeRuleDto), sqlSession);
      }
    }
  }

  public ProfileImporter getProfileImporter(String exporterKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(exporterKey, importer.getKey())) {
        return importer;
      }
    }
    return null;
  }

  private void processValidationMessages(ValidationMessages messages, NewProfileResult result) {
    BadRequestException exception = BadRequestException.of("Fail to create profile");
    for (String error : messages.getErrors()) {
      exception.addError(error);
    }
    if (!exception.errors().isEmpty()) {
      throw exception;
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
    return rulePriority.ordinal() + 1;
  }

  private ActiveRuleParamDto toActiveRuleParamDto(ActiveRuleParam activeRuleParam, ActiveRuleDto activeRuleDto) {
    return new ActiveRuleParamDto()
      .setActiveRuleId(activeRuleDto.getId())
      .setRulesParameterId(activeRuleParam.getRuleParam().getId())
      .setValue(activeRuleParam.getValue());
  }

  private void validateNewProfile(String name, String language, UserSession userSession) {
    checkPermission(userSession);
    validateName(name);
    Validation.checkMandatoryParameter(language, "language");
    if (find(name, language) != null) {
      throw BadRequestException.ofL10n(QUALITY_PROFILES_ALREADY_EXISTS);
    }
  }

  private QualityProfileDto validateRenameProfile(Integer profileId, String newName, UserSession userSession) {
    checkPermission(userSession);
    validateName(newName);
    QualityProfileDto profileDto = findNeverNull(profileId);
    if (!profileDto.getName().equals(newName) && find(newName, profileDto.getLanguage()) != null) {
      throw BadRequestException.ofL10n(QUALITY_PROFILES_ALREADY_EXISTS);
    }
    return profileDto;
  }

  private QualityProfileDto validateUpdateDefaultProfile(Integer id, UserSession userSession) {
    checkPermission(userSession);
    return findNeverNull(id);
  }

  private QualityProfileDto findNeverNull(Integer id) {
    QualityProfileDto qualityProfile = find(id);
    return checkNotNull((qualityProfile));
  }

  private QualityProfileDto findNeverNull(String name, String language) {
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
    return dao.selectByNameAndLanguage(name, language);
  }

  @CheckForNull
  private QualityProfileDto find(Integer id) {
    return dao.selectById(id);
  }

  private void validateName(String name) {
    if (Strings.isNullOrEmpty(name)) {
      throw BadRequestException.ofL10n("quality_profiles.please_type_profile_name");
    }
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

}
