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

package org.sonar.core.debt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultTechnicalDebtModel implements TechnicalDebtModel {

  private Collection<DefaultCharacteristic> rootCharacteristics;

  public DefaultTechnicalDebtModel() {
    rootCharacteristics = newArrayList();
  }

  public DefaultTechnicalDebtModel addRootCharacteristic(DefaultCharacteristic characteristic) {
    rootCharacteristics.add(characteristic);
    return this;
  }

  public List<DefaultCharacteristic> rootCharacteristics() {
    return newArrayList(Iterables.filter(rootCharacteristics, new Predicate<DefaultCharacteristic>() {
      @Override
      public boolean apply(DefaultCharacteristic input) {
        return input.isRoot();
      }
    }));
  }

  @CheckForNull
  public DefaultCharacteristic characteristicByKey(final String key) {
    return Iterables.find(characteristics(), new Predicate<DefaultCharacteristic>() {
      @Override
      public boolean apply(DefaultCharacteristic input) {
        return input.key().equals(key);
      }
    }, null);
  }

  @CheckForNull
  public DefaultCharacteristic characteristicById(final Integer id){
    return Iterables.find(characteristics(), new Predicate<DefaultCharacteristic>() {
      @Override
      public boolean apply(DefaultCharacteristic input) {
        return input.id().equals(id);
      }
    }, null);
  }

  @CheckForNull
  public DefaultRequirement requirementsByRule(final RuleKey ruleKey) {
    return Iterables.find(requirements(), new Predicate<DefaultRequirement>() {
      @Override
      public boolean apply(DefaultRequirement input) {
        return input.ruleKey().equals(ruleKey);
      }
    }, null);
  }

  @CheckForNull
  public DefaultRequirement requirementsById(final Integer id){
    return Iterables.find(requirements(), new Predicate<DefaultRequirement>() {
      @Override
      public boolean apply(DefaultRequirement input) {
        return input.id().equals(id);
      }
    }, null);
  }

  public List<DefaultCharacteristic> characteristics() {
    List<DefaultCharacteristic> flatCharacteristics = newArrayList();
    for (DefaultCharacteristic rootCharacteristic : rootCharacteristics) {
      flatCharacteristics.add(rootCharacteristic);
      for (DefaultCharacteristic characteristic : rootCharacteristic.children()) {
        flatCharacteristics.add(characteristic);
      }
    }
    return flatCharacteristics;
  }

  public List<DefaultRequirement> requirements() {
    List<DefaultRequirement> allRequirements = newArrayList();
    for (DefaultCharacteristic characteristic : characteristics()) {
      for (DefaultRequirement requirement : characteristic.requirements()) {
        allRequirements.add(requirement);
      }
    }
    return allRequirements;
  }

  public boolean isEmpty(){
    return rootCharacteristics.isEmpty();
  }

}
