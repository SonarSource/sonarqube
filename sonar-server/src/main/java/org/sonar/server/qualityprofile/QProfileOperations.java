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
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final List<ProfileExporter> exporters;
  private final List<ProfileImporter> importers;
  private final PreviewCache dryRunCache;

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, PreviewCache dryRunCache) {
    this(myBatis, dao, activeRuleDao, Lists.<ProfileExporter>newArrayList(), Lists.<ProfileImporter>newArrayList(), dryRunCache);
  }

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, List<ProfileExporter> exporters, List<ProfileImporter> importers,
                            PreviewCache dryRunCache) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.exporters = exporters;
    this.importers = importers;
    this.dryRunCache = dryRunCache;
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
    } finally {
      sqlSession.commit();
      dryRunCache.reportGlobalModification();
      return result;
    }
  }

  public void renameProfile(String name, String language, String newName, UserSession userSession) {
    QualityProfileDto qualityProfile = validateRenameProfile(name, language, newName, userSession);
    qualityProfile.setName(newName);
    dao.update(qualityProfile);
  }

  private QualityProfileDto find(String name, String language) {
    return dao.selectByNameAndLanguage(name, language);
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
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }
  }

  private QualityProfileDto validateRenameProfile(String name, String language, String newName, UserSession userSession) {
    checkPermission(userSession);
    validateName(newName);
    if (find(newName, language) != null) {
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }

    QualityProfileDto qualityProfile = find(name, language);
    if (qualityProfile == null) {
      throw new NotFoundException("This quality profile does not exists.");
    }
    return qualityProfile;
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
