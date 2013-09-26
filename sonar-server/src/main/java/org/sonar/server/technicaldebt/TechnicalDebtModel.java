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
package org.sonar.server.technicaldebt;

import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.CharacteristicProperty;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.ValidationMessages;

import java.util.List;

public class TechnicalDebtModel {

  private Model model;

  private List<Characteristic> defaultCharacteristics;

  public TechnicalDebtModel(Model model, List<Characteristic> defaultCharacteristics) {
    this.model = model;
    this.defaultCharacteristics = defaultCharacteristics;
  }

  public void mergeWith(Model with, ValidationMessages messages, RuleCache ruleCache) {
    for (Characteristic characteristic : with.getCharacteristics()) {
      if (isRequirement(characteristic)) {
        mergeRequirement(characteristic, messages, ruleCache);
      } else {
        mergeCharacteristic(characteristic, messages);
      }
    }
  }

  private Characteristic mergeCharacteristic(Characteristic characteristic, ValidationMessages messages) {
    Characteristic existingCharacteristic = model.getCharacteristicByKey(characteristic.getKey());
    if (existingCharacteristic == null) {
      if (defaultCharacteristics.contains(characteristic)) {
        existingCharacteristic = model.addCharacteristic(clone(characteristic));
        if (!characteristic.getParents().isEmpty()) {
          Characteristic parentTargetCharacteristic = mergeCharacteristic(characteristic.getParents().get(0), messages);
          parentTargetCharacteristic.addChild(existingCharacteristic);
        }
      } else {
        throw new IllegalArgumentException("The characteristic : " + characteristic.getKey() + " cannot be used as it's not available in default ones.");
      }
    }
    return existingCharacteristic;
  }

  private void mergeRequirement(Characteristic requirement, ValidationMessages messages, RuleCache ruleCache) {
    Characteristic targetRequirement = model.getCharacteristicByRule(requirement.getRule());
    if (targetRequirement == null && !requirement.getParents().isEmpty()) {
      Rule rule = ruleCache.getRule(requirement.getRule().getRepositoryKey(), requirement.getRule().getKey());
      if (rule == null) {
        messages.addWarningText("The rule " + requirement.getRule() + " does not exist.");

      } else {
        Characteristic parent = mergeCharacteristic(requirement.getParents().get(0), messages);
        requirement = model.addCharacteristic(clone(requirement));
        requirement.setRule(rule);
        parent.addChild(requirement);
      }
    }
  }

  private boolean isRequirement(Characteristic characteristic) {
    return characteristic.hasRule();
  }

  private Characteristic clone(Characteristic c) {
    Characteristic clone = Characteristic.create();
    clone.setRule(c.getRule());
    clone.setDescription(c.getDescription());
    clone.setKey(c.getKey());
    clone.setName(c.getName(), false);
    for (CharacteristicProperty property : c.getProperties()) {
      clone.setProperty(property.getKey(), property.getTextValue()).setValue(property.getValue());
    }
    return clone;
  }
}
