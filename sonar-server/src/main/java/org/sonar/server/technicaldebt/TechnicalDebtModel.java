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

public class TechnicalDebtModel {

  private Model model;

  public TechnicalDebtModel(Model model) {
    this.model = model;
  }

  public void mergeWith(Model with, ValidationMessages messages, RuleCache ruleCache) {
    for (Characteristic characteristic : with.getCharacteristics()) {
      if (isRequirement(characteristic)) {
        mergeRequirement(model, characteristic, messages, ruleCache);
      } else {
        mergeCharacteristic(model, characteristic, messages);
      }
    }
  }

  private Characteristic mergeCharacteristic(Model target, Characteristic characteristic, ValidationMessages messages) {
    Characteristic targetCharacteristic = target.getCharacteristicByKey(characteristic.getKey());
    if (targetCharacteristic == null) {
      targetCharacteristic = target.addCharacteristic(clone(characteristic));
      if (!characteristic.getParents().isEmpty()) {
        Characteristic parentTargetCharacteristic = mergeCharacteristic(target, characteristic.getParents().get(0), messages);
        parentTargetCharacteristic.addChild(targetCharacteristic);
      }
    }
    return targetCharacteristic;
  }

  private void mergeRequirement(Model target, Characteristic requirement, ValidationMessages messages,
                                RuleCache ruleCache) {
    Characteristic targetRequirement = target.getCharacteristicByRule(requirement.getRule());
    if (targetRequirement == null && !requirement.getParents().isEmpty()) {
      Rule rule = ruleCache.getRule(requirement.getRule().getRepositoryKey(), requirement.getRule().getKey());
      if (rule == null) {
        messages.addWarningText("The rule " + requirement.getRule() + " does not exist.");

      } else {
        Characteristic parent = mergeCharacteristic(target, requirement.getParents().get(0), messages);
        requirement = target.addCharacteristic(clone(requirement));
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
