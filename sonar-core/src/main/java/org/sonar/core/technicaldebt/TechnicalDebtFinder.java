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

package org.sonar.core.technicaldebt;

import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.technicaldebt.Characteristic;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class TechnicalDebtFinder implements ServerComponent, BatchComponent {

  private final CharacteristicDao dao;
  private final RuleFinder ruleFinder;

  public TechnicalDebtFinder(CharacteristicDao dao, RuleFinder ruleFinder) {
    this.dao = dao;
    this.ruleFinder = ruleFinder;
  }

  public TechnicalDebtModel findAll() {
    TechnicalDebtModel model = new TechnicalDebtModel();
    List<CharacteristicDto> dtos = dao.selectEnabledCharacteristics();
    Map<Integer, Characteristic> characteristicsById = newHashMap();

    addRootCharacteristics(model, dtos, characteristicsById);
    addCharacteristics(model, dtos, characteristicsById);
    addRequirements(model, dtos, characteristicsById);
    return model;
  }

  public TechnicalDebtModel findRootCharacteristics() {
    TechnicalDebtModel model = new TechnicalDebtModel();
    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();
    addRootCharacteristics(model, dtos, Maps.<Integer, Characteristic>newHashMap());
    return model;
  }

  private void addRootCharacteristics(TechnicalDebtModel model, List<CharacteristicDto> dtos, Map<Integer, Characteristic> characteristicsById){
    for (CharacteristicDto dto : dtos) {
      if (dto.getParentId() == null) {
        Characteristic rootCharacteristic = dto.toCharacteristic(null);
        model.addRootCharacteristic(rootCharacteristic);
        characteristicsById.put(dto.getId(), rootCharacteristic);
      }
    }
  }

  private void addCharacteristics(TechnicalDebtModel model, List<CharacteristicDto> dtos, Map<Integer, Characteristic> characteristicsById){
    for (CharacteristicDto dto : dtos) {
      if (dto.getParentId() != null && dto.getRuleId() == null) {
        Characteristic parent = characteristicsById.get(dto.getParentId());
        Characteristic characteristic = dto.toCharacteristic(parent);
        characteristicsById.put(dto.getId(), characteristic);
      }
    }
  }

  private void addRequirements(TechnicalDebtModel model, List<CharacteristicDto> dtos, Map<Integer, Characteristic> characteristicsById){
    Map<Integer, Rule> rulesById = rulesById(ruleFinder.findAll(RuleQuery.create()));
    for (CharacteristicDto dto : dtos) {
      Integer ruleId = dto.getRuleId();
      if (ruleId != null) {
        Characteristic characteristic = characteristicsById.get(dto.getParentId());
        Rule rule = rulesById.get(ruleId);
        if (rule != null) {
          RuleKey ruleKey = RuleKey.of(rule.getRepositoryKey(), rule.getKey());
          dto.toRequirement(ruleKey, characteristic);
        } else {
          dto.toRequirement(null, characteristic);
        }
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
