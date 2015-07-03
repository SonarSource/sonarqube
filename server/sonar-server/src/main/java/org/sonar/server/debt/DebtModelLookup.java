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

package org.sonar.server.debt;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.server.debt.DebtPredicates.ToDebtCharacteristic.INSTANCE;
import static org.sonar.server.debt.DebtPredicates.toDebtCharacteristic;

@ServerSide
public class DebtModelLookup {

  private final CharacteristicDao dao;

  public DebtModelLookup(CharacteristicDao dao) {
    this.dao = dao;
  }

  public List<DebtCharacteristic> rootCharacteristics() {
    return toCharacteristics(dao.selectEnabledRootCharacteristics());
  }

  public List<DebtCharacteristic> allCharacteristics() {
    return toCharacteristics(dao.selectEnabledCharacteristics());
  }

  @CheckForNull
  public DebtCharacteristic characteristicById(int id) {
    CharacteristicDto dto = dao.selectById(id);
    return dto != null ? toDebtCharacteristic(dto) : null;
  }

  @CheckForNull
  public DebtCharacteristic characteristicByKey(String key) {
    CharacteristicDto dto = dao.selectByKey(key);
    return dto != null ? toDebtCharacteristic(dto) : null;
  }

  private static List<DebtCharacteristic> toCharacteristics(Collection<CharacteristicDto> dtos) {
    return newArrayList(Iterables.transform(dtos, INSTANCE));
  }

}
