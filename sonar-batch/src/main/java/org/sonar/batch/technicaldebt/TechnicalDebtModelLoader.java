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

package org.sonar.batch.technicaldebt;

import org.sonar.api.BatchComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.internal.DefaultCharacteristic;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtModel;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class TechnicalDebtModelLoader implements BatchComponent {

  private final CharacteristicDao dao;
  private final RuleFinder ruleFinder;

  public TechnicalDebtModelLoader(CharacteristicDao dao, RuleFinder ruleFinder) {
    this.dao = dao;
    this.ruleFinder = ruleFinder;
  }

  public TechnicalDebtModel load() {
    DefaultTechnicalDebtModel model = new DefaultTechnicalDebtModel();
    List<CharacteristicDto> dtos = dao.selectEnabledCharacteristics();
    Map<Integer, DefaultCharacteristic> characteristicsById = newHashMap();

    addRootCharacteristics(model, dtos, characteristicsById);
    addCharacteristics(model, dtos, characteristicsById);
    addRequirements(model, dtos, characteristicsById);
    return model;
  }

  private void addRootCharacteristics(DefaultTechnicalDebtModel model, List<CharacteristicDto> dtos, Map<Integer, DefaultCharacteristic> characteristicsById) {
    for (CharacteristicDto dto : dtos) {
      if (dto.getParentId() == null) {
        DefaultCharacteristic rootCharacteristic = dto.toCharacteristic(null);
        model.addRootCharacteristic(rootCharacteristic);
        characteristicsById.put(dto.getId(), rootCharacteristic);
      }
    }
  }

  private void addCharacteristics(DefaultTechnicalDebtModel model, List<CharacteristicDto> dtos, Map<Integer, DefaultCharacteristic> characteristicsById) {
    for (CharacteristicDto dto : dtos) {
      if (dto.getParentId() != null && dto.getRuleId() == null) {
        DefaultCharacteristic parent = characteristicsById.get(dto.getParentId());
        DefaultCharacteristic characteristic = dto.toCharacteristic(parent);
        characteristicsById.put(dto.getId(), characteristic);
      }
    }
  }

  private void addRequirements(DefaultTechnicalDebtModel model, List<CharacteristicDto> dtos, Map<Integer, DefaultCharacteristic> characteristicsById) {
    Map<Integer, Rule> rulesById = rulesById(ruleFinder.findAll(RuleQuery.create()));
    for (CharacteristicDto dto : dtos) {
      Integer ruleId = dto.getRuleId();
      if (ruleId != null) {
        DefaultCharacteristic characteristic = characteristicsById.get(dto.getParentId());
        DefaultCharacteristic rootCharacteristic = characteristicsById.get(dto.getRootId());
        Rule rule = rulesById.get(ruleId);
        RuleKey ruleKey = RuleKey.of(rule.getRepositoryKey(), rule.getKey());
        dto.toRequirement(ruleKey, characteristic, rootCharacteristic);
      }
    }
  }

  private Map<Integer, Rule> rulesById(Collection<Rule> rules) {
    Map<Integer, Rule> rulesById = newHashMap();
    for (Rule rule : rules) {
      rulesById.put(rule.getId(), rule);
    }
    return rulesById;
  }

}
