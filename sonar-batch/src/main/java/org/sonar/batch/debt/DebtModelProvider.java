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

package org.sonar.batch.debt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.debt.DebtCharacteristic;
import org.sonar.api.batch.debt.DebtModel;
import org.sonar.api.batch.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import javax.annotation.Nullable;
import java.util.List;

public class DebtModelProvider extends ProviderAdapter {

  private DebtModel model;

  public DebtModel provide(CharacteristicDao dao) {
    if (model == null) {
      Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Load technical debt model");
      model = load(dao);
      profiler.stopDebug();
    }
    return model;
  }

  private DebtModel load(CharacteristicDao dao) {
    DefaultDebtModel debtModel = new DefaultDebtModel();

    List<CharacteristicDto> allCharacteristics = dao.selectEnabledCharacteristics();
    for (CharacteristicDto dto : allCharacteristics) {
      Integer parentId = dto.getParentId();
      if (parentId == null) {
        debtModel.addCharacteristic(toDebtCharacteristic(dto));
      } else {
        debtModel.addSubCharacteristic(toDebtCharacteristic(dto), characteristicById(parentId, allCharacteristics).getKey());
      }
    }
    return debtModel;
  }

  private static CharacteristicDto characteristicById(final int id, List<CharacteristicDto> allCharacteristics) {
    return Iterables.find(allCharacteristics, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(@Nullable CharacteristicDto input) {
        return input != null && id == input.getId();
      }
    });
  }

  private static DebtCharacteristic toDebtCharacteristic(CharacteristicDto characteristic) {
    return new DefaultDebtCharacteristic()
      .setId(characteristic.getId())
      .setKey(characteristic.getKey())
      .setName(characteristic.getName())
      .setOrder(characteristic.getOrder())
      .setParentId(characteristic.getParentId());
  }

}
