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

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.sonar.api.server.debt.DebtCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

// TODO Maybe should be an inner class of DebtCharacteristicsXMLImporter? Will see following how will be implemented the Backup feature
public class DebtModel {

  private Multimap<String, DebtCharacteristic> characteristicsByRootKey;

  public DebtModel() {
    characteristicsByRootKey = ArrayListMultimap.create();
  }

  public DebtModel addRootCharacteristic(DebtCharacteristic characteristic) {
    characteristicsByRootKey.put(null, characteristic);
    return this;
  }

  public DebtModel addSubCharacteristic(DebtCharacteristic subCharacteristic, String characteristicKey) {
    characteristicsByRootKey.put(characteristicKey, subCharacteristic);
    return this;
  }

  public List<DebtCharacteristic> rootCharacteristics() {
    return newArrayList(characteristicsByRootKey.get(null));
  }

  public List<DebtCharacteristic> subCharacteristics(String characteristicKey) {
    return newArrayList(characteristicsByRootKey.get(characteristicKey));
  }

  @CheckForNull
  public DebtCharacteristic characteristicByKey(final String key) {
    return Iterables.find(characteristicsByRootKey.values(), new Predicate<DebtCharacteristic>() {
      @Override
      public boolean apply(DebtCharacteristic input) {
        return key.equals(input.key());
      }
    }, null);
  }

}
