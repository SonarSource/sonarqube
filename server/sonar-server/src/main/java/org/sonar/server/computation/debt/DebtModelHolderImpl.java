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

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class DebtModelHolderImpl implements MutableDebtModelHolder {

  private final Multimap<Characteristic, Characteristic> subCharacteristicsByRootCharacteristic = ArrayListMultimap.create();
  private final Map<String, Characteristic> characteristicByKey = new HashMap<>();

  @Override
  public void addCharacteristics(Characteristic rootCharacteristic, Iterable<? extends Characteristic> subCharacteristics) {
    requireNonNull(rootCharacteristic, "rootCharacteristic cannot be null");
    requireNonNull(subCharacteristics, "subCharacteristics cannot be null");
    checkArgument(subCharacteristics.iterator().hasNext(), "subCharacteristics cannot be empty");
    subCharacteristicsByRootCharacteristic.putAll(rootCharacteristic, subCharacteristics);

    characteristicByKey.put(rootCharacteristic.getKey(), rootCharacteristic);
    for (Characteristic characteristic : subCharacteristics) {
      characteristicByKey.put(characteristic.getKey(), characteristic);
    }
  }

  @Override
  public Set<Characteristic> findRootCharacteristics() {
    checkCharacteristicsAreInitialized();
    return ImmutableSet.copyOf(subCharacteristicsByRootCharacteristic.keySet());
  }

  @Override
  public Collection<Characteristic> findSubCharacteristicsByRootKey(String rootCharacteristicKey) {
    checkCharacteristicsAreInitialized();
    Characteristic rootCharacteristic = characteristicByKey.get(rootCharacteristicKey);
    if (rootCharacteristic == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(subCharacteristicsByRootCharacteristic.get(rootCharacteristic));
  }

  @Override
  public Optional<Characteristic> getCharacteristicByKey(String key) {
    checkCharacteristicsAreInitialized();
    return fromNullable(characteristicByKey.get(key));
  }

  private void checkCharacteristicsAreInitialized() {
    checkState(!characteristicByKey.isEmpty(), "Characteristics have not been initialized yet");
  }
}
