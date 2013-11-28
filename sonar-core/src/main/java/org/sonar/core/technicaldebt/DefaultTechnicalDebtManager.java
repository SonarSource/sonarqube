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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.WorkUnit;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.technicaldebt.server.TechnicalDebtManager;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultTechnicalDebtManager implements TechnicalDebtManager {

  private final CharacteristicDao dao;

  public DefaultTechnicalDebtManager(CharacteristicDao dao) {
    this.dao = dao;
  }

  public List<Characteristic> findRootCharacteristics() {
    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();
    List<Characteristic> characteristics = newArrayList();
    for (CharacteristicDto dto : dtos) {
      characteristics.add(toCharacteristic(dto, null));
    }
    return characteristics;
  }

  public Characteristic findCharacteristicById(Integer id) {
    return toCharacteristic(dao.selectById(id), null);
  }

  public Characteristic findRequirementByRule(Rule rule) {
    CharacteristicDto requirementDto = dao.selectByRuleId(rule.getId());
    return toCharacteristic(requirementDto, RuleKey.of(rule.getRepositoryKey(), rule.getKey()));
  }

  private static Characteristic toCharacteristic(CharacteristicDto dto, @Nullable RuleKey ruleKey) {
    return new Characteristic()
      .setId(dto.getId())
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setOrder(dto.getOrder())
      .setParentId(dto.getParentId())
      .setRootId(dto.getRootId())
      .setRuleKey(ruleKey)
      .setFunction(dto.getFunction())
      .setFactor(WorkUnit.create(dto.getFactorValue(), dto.getFactorUnit()))
      .setOffset(WorkUnit.create(dto.getOffsetValue(), dto.getOffsetUnit()));
  }

}
