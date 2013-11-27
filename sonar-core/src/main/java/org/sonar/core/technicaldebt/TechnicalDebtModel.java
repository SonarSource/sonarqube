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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.Characteristic;
import org.sonar.api.technicaldebt.Requirement;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TechnicalDebtModel {

  private Collection<Characteristic> rootCharacteristics;

  public TechnicalDebtModel() {
    rootCharacteristics = newArrayList();
  }

  public TechnicalDebtModel addRootCharacteristic(Characteristic characteristic) {
    rootCharacteristics.add(characteristic);
    return this;
  }

  public List<Characteristic> rootCharacteristics() {
    return newArrayList(Iterables.filter(rootCharacteristics, new Predicate<Characteristic>() {
      @Override
      public boolean apply(Characteristic input) {
        return input.isRoot();
      }
    }));
  }

  @CheckForNull
  public Characteristic characteristicByKey(final String key) {
    return Iterables.find(characteristics(), new Predicate<Characteristic>() {
      @Override
      public boolean apply(Characteristic input) {
        return input.key().equals(key);
      }
    }, null);
  }

  @CheckForNull
  public Characteristic characteristicById(final Integer id){
    return Iterables.find(characteristics(), new Predicate<Characteristic>() {
      @Override
      public boolean apply(Characteristic input) {
        return input.id().equals(id);
      }
    }, null);
  }

  @CheckForNull
  public Requirement requirementsByRule(final RuleKey ruleKey) {
    return Iterables.find(requirements(), new Predicate<Requirement>() {
      @Override
      public boolean apply(Requirement input) {
        return input.ruleKey().equals(ruleKey);
      }
    }, null);
  }

  @CheckForNull
  public Requirement requirementsById(final Integer id){
    return Iterables.find(requirements(), new Predicate<Requirement>() {
      @Override
      public boolean apply(Requirement input) {
        return input.id().equals(id);
      }
    }, null);
  }

  public List<Characteristic> characteristics() {
    List<Characteristic> flatCharacteristics = newArrayList();
    for (Characteristic rootCharacteristic : rootCharacteristics) {
      flatCharacteristics.add(rootCharacteristic);
      for (Characteristic characteristic : rootCharacteristic.children()) {
        flatCharacteristics.add(characteristic);
      }
    }
    return flatCharacteristics;
  }

  public List<Requirement> requirements() {
    List<Requirement> allRequirements = newArrayList();
    for (Characteristic characteristic : characteristics()) {
      for (Requirement requirement : characteristic.requirements()) {
        allRequirements.add(requirement);
      }
    }
    return allRequirements;
  }

  public boolean isEmpty(){
    return rootCharacteristics.isEmpty();
  }

}
