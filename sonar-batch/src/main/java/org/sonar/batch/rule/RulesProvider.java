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

package org.sonar.batch.rule;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.internal.NewRule;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.rule.RemediationFunction;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Loads all enabled and non manual rules
 */
public class RulesProvider extends ProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(RulesProvider.class);

  private Rules singleton = null;

  public Rules provide(RuleDao ruleDao, TechnicalDebtModel debtModel, Durations durations) {
    if (singleton == null) {
      TimeProfiler profiler = new TimeProfiler(LOG).start("Loading rules");
      singleton = load(ruleDao, debtModel, durations);
      profiler.stop();
    }
    return singleton;
  }

  private Rules load(RuleDao ruleDao, TechnicalDebtModel debtModel, Durations durations) {
    RulesBuilder rulesBuilder = new RulesBuilder();

    List<RuleParamDto> ruleParamDtos = ruleDao.selectParameters();
    ListMultimap<Integer, RuleParamDto> paramDtosByRuleId = ArrayListMultimap.create();
    for (RuleParamDto dto : ruleParamDtos) {
      paramDtosByRuleId.put(dto.getRuleId(), dto);
    }
    for (RuleDto ruleDto : ruleDao.selectEnablesAndNonManual()) {
      RuleKey ruleKey = RuleKey.of(ruleDto.getRepositoryKey(), ruleDto.getRuleKey());
      NewRule newRule = rulesBuilder.add(ruleKey)
        .setId(ruleDto.getId())
        .setName(ruleDto.getName())
        .setSeverity(ruleDto.getSeverityString())
        .setDescription(ruleDto.getDescription())
        .setStatus(RuleStatus.valueOf(ruleDto.getStatus()));
      // TODO should we set metadata ?

      if (ruleDto.hasCharacteristic()) {
        newRule.setCharacteristic(characteristic(ruleDto, ruleKey, debtModel).key());
        setFunction(ruleDto, newRule, ruleKey, durations);
      }

      for (RuleParamDto ruleParamDto : paramDtosByRuleId.get(ruleDto.getId())) {
        newRule.addParam(ruleParamDto.getName())
          .setDescription(ruleParamDto.getDescription());
      }
    }
    return rulesBuilder.build();
  }

  private void setFunction(RuleDto ruleDto, NewRule newRule, RuleKey ruleKey, Durations durations) {
    String function = ruleDto.getRemediationFunction();
    String factor = ruleDto.getRemediationFactor();
    String offset = ruleDto.getRemediationOffset();

    String defaultFunction = ruleDto.getDefaultRemediationFunction();
    String defaultFactor = ruleDto.getDefaultRemediationFactor();
    String defaultOffset = ruleDto.getDefaultRemediationOffset();

    if (function != null) {
      newRule.setFunction(function(function, ruleKey));
      newRule.setFactor(factor != null ? durations.decode(factor) : null);
      newRule.setOffset(offset != null ? durations.decode(offset) : null);
    } else {
      newRule.setFunction(function(defaultFunction, ruleKey));
      newRule.setFactor(defaultFactor != null ? durations.decode(defaultFactor) : null);
      newRule.setOffset(defaultOffset != null ? durations.decode(defaultOffset) : null);
    }
  }

  private Characteristic characteristic(RuleDto ruleDto, RuleKey ruleKey, TechnicalDebtModel debtModel) {
    Integer characteristicId = ruleDto.getCharacteristicId();
    Integer defaultCharacteristicId = ruleDto.getDefaultCharacteristicId();
    Integer effectiveCharacteristicId = characteristicId != null ? characteristicId : defaultCharacteristicId;
    Characteristic characteristic = debtModel.characteristicById(effectiveCharacteristicId);
    if (characteristic == null) {
      throw new IllegalStateException(String.format("Characteristic id '%s' on rule '%s' has not been found", effectiveCharacteristicId, ruleKey));
    }
    return characteristic;
  }

  private RemediationFunction function(@Nullable String function, RuleKey ruleKey) {
    if (function == null) {
      throw new IllegalStateException(String.format("Remediation function should not be null on rule '%s'", ruleKey));
    }
    return RemediationFunction.valueOf(function);
  }
}
