/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.base.Splitter;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.util.TypeValidations;

import javax.annotation.CheckForNull;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileActiveRuleOperations implements ServerComponent {

  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final QualityProfileDao profileDao;
  private final TypeValidations typeValidations;

  public QProfileActiveRuleOperations(ActiveRuleDao activeRuleDao, RuleDao ruleDao, QualityProfileDao profileDao,
                                      TypeValidations typeValidations) {
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.profileDao = profileDao;
    this.typeValidations = typeValidations;
  }

  ActiveRuleDto createActiveRule(QualityProfileKey profileKey, RuleKey ruleKey, String severity, DbSession session) {
    RuleDto ruleDto = ruleDao.getNullableByKey(session, ruleKey);
    QualityProfileDto profileDto = profileDao.selectByNameAndLanguage(profileKey.name(), profileKey.lang(), session);
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setSeverity(severity);
    activeRuleDao.insert(session, activeRule);

    List<RuleParamDto> ruleParams = ruleDao.findRuleParamsByRuleKey(session, ruleKey);
    List<ActiveRuleParamDto> activeRuleParams = newArrayList();
    for (RuleParamDto ruleParam : ruleParams) {
      ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(ruleParam)
        .setKey(ruleParam.getName())
        .setValue(ruleParam.getDefaultValue());
      activeRuleParams.add(activeRuleParam);
      activeRuleDao.addParam(session, activeRule, activeRuleParam);
    }
    return activeRule;
  }

  void updateActiveRuleParam(ActiveRuleDto activeRule, String key, String sanitizedValue, DbSession session) {
    RuleParamDto ruleParam = findRuleParamNotNull(activeRule.getRulId(), key, session);
    ActiveRuleParamDto activeRuleParam = findActiveRuleParamNotNull(activeRule.getId(), key, session);
    validateParam(ruleParam, sanitizedValue);

    activeRuleParam.setValue(sanitizedValue);
    activeRuleDao.updateParam(session, activeRule, activeRuleParam);
  }


  private void validateParam(RuleParamDto ruleParam, String value) {
    RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
    if (ruleParamType.multiple()) {
      List<String> values = newArrayList(Splitter.on(",").split(value));
      typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
    } else {
      typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
    }
  }


  private RuleParamDto findRuleParamNotNull(Integer ruleId, String key, DbSession session) {
    RuleDto rule = ruleDao.getById(session, ruleId);
    RuleParamDto ruleParam = ruleDao.getRuleParamByRuleAndParamKey(session, rule, key);
    if (ruleParam == null) {
      throw new IllegalArgumentException("No rule param found");
    }
    return ruleParam;
  }

  @CheckForNull
  private ActiveRuleParamDto findActiveRuleParam(int activeRuleId, String key, DbSession session) {
    ActiveRuleDto activeRule = activeRuleDao.getById(session, activeRuleId);
    return activeRuleDao.getParamsByActiveRuleAndKey(session, activeRule, key);
  }

  private ActiveRuleParamDto findActiveRuleParamNotNull(int activeRuleId, String key, DbSession session) {
    ActiveRuleParamDto activeRuleParam = findActiveRuleParam(activeRuleId, key, session);
    if (activeRuleParam == null) {
      throw new NotFoundException(String.format("No active rule parameter '%s' has been found on active rule id '%s'", key, activeRuleId));
    }
    return activeRuleParam;
  }

}
