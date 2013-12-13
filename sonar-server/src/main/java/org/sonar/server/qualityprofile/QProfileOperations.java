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
import org.sonar.core.qualityprofile.db.*;
import org.sonar.server.issue.Result;
import org.sonar.server.user.UserSession;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class QProfileOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final List<ProfileExporter> exporters;
  private final List<ProfileImporter> importers;

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao) {
    this(myBatis, dao, activeRuleDao, Lists.<ProfileExporter>newArrayList(), Lists.<ProfileImporter>newArrayList());
  }

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, List<ProfileExporter> exporters, List<ProfileImporter> importers) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.exporters = exporters;
    this.importers = importers;
  }

  public Result<QProfile> newProfile(String name, String language, UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language);
    dao.insert(dto);
    return Result.of(QProfile.from(dto));
  }

  public Result<QProfile> newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // TODO check name not already exists

    SqlSession sqlSession = myBatis.openSession();
    Result<QProfile> result = Result.of();
    try {
      QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language);
      dao.insert(dto, sqlSession);
      for (Map.Entry<String, String> entry : xmlProfilesByPlugin.entrySet()) {
        importProfile(dto, entry.getKey(), entry.getValue(), result, sqlSession);
      }
      result.set(QProfile.from(dto));
    } finally {
      sqlSession.commit();
      return result;
    }
  }

  private void importProfile(QualityProfileDto qualityProfileDto, String pluginKey, String xmlProfile, Result<QProfile> result, SqlSession sqlSession) {
    ProfileImporter importer = getProfileImporter(pluginKey);
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new StringReader(xmlProfile), messages);
    completeErrorResult(result, messages);

    if (result.ok()) {
      for (ActiveRule activeRule : profile.getActiveRules()) {
        ActiveRuleDto activeRuleDto = toActiveRuleDto(activeRule, qualityProfileDto);
        activeRuleDao.insert(activeRuleDto, sqlSession);
        for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
          activeRuleDao.insert(toActiveRuleParamDto(activeRuleParam, activeRuleDto), sqlSession);
        }
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

  private void completeErrorResult(Result<QProfile> result, ValidationMessages messages) {
    for (String error : messages.getErrors()) {
      result.addError(error);
    }
    for (String warning : messages.getWarnings()) {
      result.addError(warning);
    }
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

}
