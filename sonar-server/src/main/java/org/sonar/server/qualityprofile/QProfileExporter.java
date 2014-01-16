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
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;

import javax.annotation.CheckForNull;

import java.io.StringReader;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileExporter implements ServerComponent {

  private final ActiveRuleDao activeRuleDao;
  private final List<ProfileImporter> importers;
  private final RuleRegistry ruleRegistry;

  /**
   * Used by pico when no plugin provide profile exporter / importer
   */
  public QProfileExporter(ActiveRuleDao activeRuleDao, RuleRegistry ruleRegistry) {
    this(activeRuleDao, Lists.<ProfileImporter>newArrayList(), ruleRegistry);
  }

  public QProfileExporter(ActiveRuleDao activeRuleDao, List<ProfileImporter> importers, RuleRegistry ruleRegistry) {
    this.activeRuleDao = activeRuleDao;
    this.importers = importers;
    this.ruleRegistry = ruleRegistry;
  }

  public QProfileResult importXml(QProfile profile, String pluginKey, String xml, SqlSession session) {
    QProfileResult result = new QProfileResult();
    ValidationMessages messages = ValidationMessages.create();
    ProfileImporter importer = getProfileImporter(pluginKey);
    RulesProfile rulesProfile = importer.importProfile(new StringReader(xml), messages);
    importProfile(profile.id(), rulesProfile, session);
    processValidationMessages(messages, result);
    return result;
  }

  private void importProfile(int profileId, RulesProfile rulesProfile, SqlSession sqlSession) {
    List<ActiveRuleDto> activeRuleDtos = newArrayList();
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    for (ActiveRule activeRule : rulesProfile.getActiveRules()) {
      ActiveRuleDto activeRuleDto = toActiveRuleDto(activeRule, profileId);
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

  @CheckForNull
  private ProfileImporter getProfileImporter(String exporterKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(exporterKey, importer.getKey())) {
        return importer;
      }
    }
    return null;
  }

  private void processValidationMessages(ValidationMessages messages, QProfileResult result) {
    if (!messages.getErrors().isEmpty()) {
      List<BadRequestException.Message> errors = newArrayList();
      for (String error : messages.getErrors()) {
        errors.add(BadRequestException.Message.of(error));
      }
      throw BadRequestException.of("Fail to import profile", errors);
    }
    result.setWarnings(messages.getWarnings());
    result.setInfos(messages.getInfos());
  }

  private ActiveRuleDto toActiveRuleDto(ActiveRule activeRule, int profileId) {
    return new ActiveRuleDto()
      .setProfileId(profileId)
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

}
