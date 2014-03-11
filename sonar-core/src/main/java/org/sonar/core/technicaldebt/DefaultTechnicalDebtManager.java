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

package org.sonar.core.technicaldebt;


import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.technicaldebt.server.TechnicalDebtManager;
import org.sonar.api.technicaldebt.server.internal.DefaultCharacteristic;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * TODO This class should be replaced or created by a TechnicalDebtManagerBuilder
 */
public class DefaultTechnicalDebtManager implements TechnicalDebtManager {

  private final CharacteristicDao dao;

  public DefaultTechnicalDebtManager(CharacteristicDao dao) {
    this.dao = dao;
  }

  public List<Characteristic> findRootCharacteristics() {
    List<CharacteristicDto> dtos = dao.selectEnabledRootCharacteristics();
    List<Characteristic> characteristics = newArrayList();
    for (CharacteristicDto dto : dtos) {
      characteristics.add(toCharacteristic(dto));
    }
    return characteristics;
  }

  @CheckForNull
  public Characteristic findCharacteristicById(Integer id) {
    CharacteristicDto dto = dao.selectById(id);
    if (dto != null) {
      return toCharacteristic(dto);
    }
    return null;
  }

  /**
   * @deprecated since 4.3. Always return null
   */
  @Deprecated
  @CheckForNull
  public Characteristic findRequirementByRuleId(int ruleId) {
    return null;
  }

  @CheckForNull
  public Characteristic findRequirementByRule(Rule rule) {
    return null;
  }

  private static Characteristic toCharacteristic(CharacteristicDto dto) {
    return new DefaultCharacteristic()
      .setId(dto.getId())
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setOrder(dto.getOrder())
      .setParentId(dto.getParentId());
  }

}
