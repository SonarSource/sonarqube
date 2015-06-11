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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebtModelHolderImpl implements MutableDebtModelHolder {

  private Multimap<Characteristic, Characteristic> subCharacteristicsByRootCharacteristic = ArrayListMultimap.create();
  private Map<String, Characteristic> characteristicByKey = new HashMap<>();

  @Override
  public void addCharacteristics(Characteristic rootCharacteristic, List<? extends Characteristic> subCharacteristics) {
    Preconditions.checkNotNull(rootCharacteristic, "rootCharacteristic cannot be null");
    Preconditions.checkNotNull(subCharacteristics, "subCharacteristics cannot be null");
    Preconditions.checkState(!subCharacteristics.isEmpty(), "subCharacteristics cannot be empty");
    subCharacteristicsByRootCharacteristic.putAll(rootCharacteristic, subCharacteristics);

    characteristicByKey.put(rootCharacteristic.key(), rootCharacteristic);
    for (Characteristic characteristic : subCharacteristics) {
      characteristicByKey.put(characteristic.key(), characteristic);
    }
  }

  @Override
  public Collection<Characteristic> rootCharacteristics() {
    checkCharacteristicsAreInitialized();
    return ImmutableSet.copyOf(subCharacteristicsByRootCharacteristic.keySet());
  }

  @Override
  public Collection<Characteristic> subCharacteristicsByRootKey(String rootCharacteristicKey) {
    checkCharacteristicsAreInitialized();
    Characteristic rootCharacteristic = characteristicByKey.get(rootCharacteristicKey);
    if (rootCharacteristic == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(subCharacteristicsByRootCharacteristic.get(rootCharacteristic));
  }

  @Override
  public Characteristic characteristicByKey(String key) {
    checkCharacteristicsAreInitialized();
    return characteristicByKey.get(key);
  }

  private void checkCharacteristicsAreInitialized() {
    Preconditions.checkState(!characteristicByKey.isEmpty(), "Characteristics have not been initialized yet");
  }
}
