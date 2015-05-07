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

package org.sonar.core.technicaldebt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.debt.DebtCharacteristic;
import org.sonar.api.batch.debt.DebtModel;
import org.sonar.api.batch.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@RequiresDB
public class DefaultTechnicalDebtModel implements TechnicalDebtModel {

  private final DebtModel model;

  public DefaultTechnicalDebtModel(DebtModel model) {
    this.model = model;
  }

  public List<DefaultCharacteristic> rootCharacteristics() {
    return newArrayList(Iterables.filter(characteristics(), new Predicate<DefaultCharacteristic>() {
      @Override
      public boolean apply(DefaultCharacteristic input) {
        return input.isRoot();
      }
    }));
  }

  @Override
  @CheckForNull
  public DefaultCharacteristic characteristicByKey(final String key) {
    return Iterables.find(characteristics(), new Predicate<DefaultCharacteristic>() {
      @Override
      public boolean apply(DefaultCharacteristic input) {
        return input.key().equals(key);
      }
    }, null);
  }

  @Override
  @CheckForNull
  public DefaultCharacteristic characteristicById(final Integer id) {
    return Iterables.find(characteristics(), new Predicate<DefaultCharacteristic>() {
      @Override
      public boolean apply(DefaultCharacteristic input) {
        return input.id().equals(id);
      }
    }, null);
  }

  @Override
  @CheckForNull
  public DefaultRequirement requirementsByRule(final RuleKey ruleKey) {
    return null;
  }

  @Override
  @CheckForNull
  public DefaultRequirement requirementsById(final Integer id) {
    return null;
  }

  @Override
  public List<DefaultCharacteristic> characteristics() {
    List<DefaultCharacteristic> flatCharacteristics = newArrayList();
    for (DebtCharacteristic characteristic : model.characteristics()) {
      DefaultCharacteristic root = toDefaultCharacteristic((DefaultDebtCharacteristic) characteristic, null);
      flatCharacteristics.add(root);
      for (DebtCharacteristic subCharacteristic : model.subCharacteristics(characteristic.key())) {
        flatCharacteristics.add(toDefaultCharacteristic((DefaultDebtCharacteristic) subCharacteristic, root));
      }
    }
    return flatCharacteristics;
  }

  @Override
  public List<DefaultRequirement> requirements() {
    return Collections.emptyList();
  }

  public boolean isEmpty() {
    return model.allCharacteristics().isEmpty();
  }

  private static DefaultCharacteristic toDefaultCharacteristic(DefaultDebtCharacteristic debtCharacteristic, @Nullable DefaultCharacteristic parentCharacteristic) {
    return new DefaultCharacteristic()
      .setId(debtCharacteristic.id())
      .setKey(debtCharacteristic.key())
      .setName(debtCharacteristic.name())
      .setOrder(debtCharacteristic.order())
      .setParent(parentCharacteristic)
      .setRoot(parentCharacteristic)
      .setCreatedAt(debtCharacteristic.createdAt())
      .setUpdatedAt(debtCharacteristic.updatedAt());
  }

}
