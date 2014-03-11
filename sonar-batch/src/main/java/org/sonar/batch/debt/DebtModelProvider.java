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

package org.sonar.batch.debt;

import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtModel;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class DebtModelProvider extends ProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(DebtModelProvider.class);

  private TechnicalDebtModel model;

  public TechnicalDebtModel provide(CharacteristicDao dao) {
    if (model == null) {
      TimeProfiler profiler = new TimeProfiler(LOG).start("Loading technical debt model");
      model = load(dao);
      profiler.stop();
    }
    return model;
  }

  private TechnicalDebtModel load(CharacteristicDao dao) {
    DefaultTechnicalDebtModel model = new DefaultTechnicalDebtModel();
    List<CharacteristicDto> dtos = dao.selectEnabledCharacteristics();
    Map<Integer, DefaultCharacteristic> characteristicsById = newHashMap();

    addRootCharacteristics(model, dtos, characteristicsById);
    addCharacteristics(dtos, characteristicsById);
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

  private void addCharacteristics(List<CharacteristicDto> dtos, Map<Integer, DefaultCharacteristic> characteristicsById) {
    for (CharacteristicDto dto : dtos) {
      if (dto.getParentId() != null) {
        DefaultCharacteristic parent = characteristicsById.get(dto.getParentId());
        DefaultCharacteristic characteristic = dto.toCharacteristic(parent);
        characteristicsById.put(dto.getId(), characteristic);
      }
    }
  }
}
