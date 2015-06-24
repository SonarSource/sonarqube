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

package org.sonar.server.computation.debt;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class DebtModelHolderImpl implements MutableDebtModelHolder {

  private final Map<Integer, Characteristic> characteristicById = new HashMap<>();

  @Override
  public void addCharacteristics(Characteristic rootCharacteristic, Iterable<? extends Characteristic> subCharacteristics) {
    requireNonNull(rootCharacteristic, "rootCharacteristic cannot be null");
    requireNonNull(subCharacteristics, "subCharacteristics cannot be null");
    checkArgument(subCharacteristics.iterator().hasNext(), "subCharacteristics cannot be empty");

    characteristicById.put(rootCharacteristic.getId(), rootCharacteristic);
    for (Characteristic characteristic : subCharacteristics) {
      characteristicById.put(characteristic.getId(), characteristic);
    }
  }

  @Override
  public Characteristic getCharacteristicById(int id) {
    checkCharacteristicsAreInitialized();
    Characteristic characteristic = characteristicById.get(id);
    if (characteristic == null) {
      throw new IllegalStateException("Debt characteristic with id [" + id + "] does not exist");
    }
    return characteristic;
  }

  private void checkCharacteristicsAreInitialized() {
    checkState(!characteristicById.isEmpty(), "Characteristics have not been initialized yet");
  }
}
