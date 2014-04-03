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

package org.sonar.api.batch.debt.internal;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.sonar.api.batch.debt.DebtCharacteristic;
import org.sonar.api.batch.debt.DebtModel;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultDebtModel implements DebtModel {

  /**
   * Sub-characteristics list can be retrieved with the characteristic key
   * Characteristics list can be retrieved by with the null key
   */
  private Multimap<String, DebtCharacteristic> characteristicsByKey;

  public DefaultDebtModel() {
    characteristicsByKey = ArrayListMultimap.create();
  }

  public DefaultDebtModel addCharacteristic(DebtCharacteristic characteristic) {
    characteristicsByKey.put(null, characteristic);
    return this;
  }

  public DefaultDebtModel addSubCharacteristic(DebtCharacteristic subCharacteristic, String characteristicKey) {
    characteristicsByKey.put(characteristicKey, subCharacteristic);
    return this;
  }

  @Override
  public List<DebtCharacteristic> characteristics() {
    return newArrayList(characteristicsByKey.get(null));
  }

  @Override
  public List<DebtCharacteristic> subCharacteristics(String characteristicKey) {
    return newArrayList(characteristicsByKey.get(characteristicKey));
  }

  @Override
  public List<DebtCharacteristic> allCharacteristics() {
    return newArrayList(characteristicsByKey.values());
  }

  @Override
  @CheckForNull
  public DebtCharacteristic characteristicByKey(final String key) {
    return Iterables.find(characteristicsByKey.values(), new Predicate<DebtCharacteristic>() {
      @Override
      public boolean apply(DebtCharacteristic input) {
        return key.equals(input.key());
      }
    }, null);
  }

  @CheckForNull
  public DebtCharacteristic characteristicById(final int id) {
    return Iterables.find(characteristicsByKey.values(), new Predicate<DebtCharacteristic>() {
      @Override
      public boolean apply(DebtCharacteristic input) {
        return id == ((DefaultDebtCharacteristic) input).id();
      }
    }, null);
  }
}
