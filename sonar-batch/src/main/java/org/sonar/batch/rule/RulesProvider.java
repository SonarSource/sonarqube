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
import org.sonar.api.batch.rule.DebtRemediationFunction;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.internal.NewRule;
import org.sonar.api.batch.rule.internal.RulesBuilder;
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

      if (hasCharacteristic(ruleDto)) {
        newRule.setDebtCharacteristic(effectiveCharacteristic(ruleDto, ruleKey, debtModel).key());
        newRule.setDebtRemediationFunction(effectiveFunction(ruleDto, ruleKey, durations));
      }

      for (RuleParamDto ruleParamDto : paramDtosByRuleId.get(ruleDto.getId())) {
        newRule.addParam(ruleParamDto.getName())
          .setDescription(ruleParamDto.getDescription());
      }
    }
    return rulesBuilder.build();
  }

  private Characteristic effectiveCharacteristic(RuleDto ruleDto, RuleKey ruleKey, TechnicalDebtModel debtModel) {
    Integer subCharacteristicId = ruleDto.getSubCharacteristicId();
    Integer defaultSubCharacteristicId = ruleDto.getDefaultSubCharacteristicId();
    Integer effectiveSubCharacteristicId = subCharacteristicId != null ? subCharacteristicId : defaultSubCharacteristicId;
    Characteristic subCharacteristic = debtModel.characteristicById(effectiveSubCharacteristicId);
    if (subCharacteristic == null) {
      throw new IllegalStateException(String.format("Sub characteristic id '%s' on rule '%s' has not been found", effectiveSubCharacteristicId, ruleKey));
    }
    return subCharacteristic;
  }

  private DebtRemediationFunction effectiveFunction(RuleDto ruleDto, RuleKey ruleKey, Durations durations) {
    String function = ruleDto.getRemediationFunction();
    String defaultFunction = ruleDto.getDefaultRemediationFunction();
    if (function != null) {
      return createDebtRemediationFunction(function, ruleDto.getRemediationCoefficient(), ruleDto.getRemediationOffset(), durations);
    } else if (defaultFunction != null) {
      return createDebtRemediationFunction(defaultFunction, ruleDto.getDefaultRemediationCoefficient(), ruleDto.getDefaultRemediationOffset(), durations);
    } else {
      throw new IllegalStateException(String.format("Remediation function should not be null on rule '%s'", ruleKey));
    }
  }

  private DebtRemediationFunction createDebtRemediationFunction(String function, @Nullable String factor, @Nullable String offset, Durations durations) {
    return DebtRemediationFunction.create(DebtRemediationFunction.Type.valueOf(function),
      factor != null ? durations.decode(factor) : null,
      offset != null ? durations.decode(offset) : null);
  }

  /**
   * Return true is the characteristic has not been overridden and a default characteristic is existing or
   * if the characteristic has been overridden but is not disabled
   */
  private boolean hasCharacteristic(RuleDto ruleDto){
    Integer subCharacteristicId = ruleDto.getSubCharacteristicId();
    return (subCharacteristicId == null && ruleDto.getDefaultSubCharacteristicId() != null) ||
      (subCharacteristicId != null && !RuleDto.DISABLED_CHARACTERISTIC_ID.equals(subCharacteristicId));
  }

}
