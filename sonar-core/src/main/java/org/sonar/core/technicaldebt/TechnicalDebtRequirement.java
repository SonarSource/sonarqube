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

import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.rules.Rule;
import org.sonar.core.technicaldebt.functions.LinearFunction;
import org.sonar.core.technicaldebt.functions.LinearWithOffsetFunction;
import org.sonar.core.technicaldebt.functions.LinearWithThresholdFunction;

public class TechnicalDebtRequirement implements Characteristicable {

  public static final String PROPERTY_REMEDIATION_FUNCTION = "remediationFunction";
  public static final String PROPERTY_REMEDIATION_FACTOR = "remediationFactor";
  public static final String PROPERTY_OFFSET = "offset";

  private Rule rule;
  private TechnicalDebtCharacteristic parent;
  private org.sonar.api.qualitymodel.Characteristic characteristic;
  private String function;
  private WorkUnit factor;
  private WorkUnit offset;

  public TechnicalDebtRequirement(Characteristic requirement, TechnicalDebtCharacteristic parent) {
    this.characteristic = requirement;
    this.rule = requirement.getRule();
    this.parent = parent;

    initFunction();
    initFactor();
    initOffset();
  }

  private void initFunction() {
    function = characteristic.getPropertyTextValue(PROPERTY_REMEDIATION_FUNCTION, LinearFunction.FUNCTION_LINEAR);
  }

  private void initFactor() {
    factor = WorkUnit.create(characteristic.getPropertyValue(PROPERTY_REMEDIATION_FACTOR, null),
      characteristic.getPropertyTextValue(PROPERTY_REMEDIATION_FACTOR, null));
  }

  private void initOffset() {
    if (LinearWithOffsetFunction.FUNCTION_LINEAR_WITH_OFFSET.equals(function) || LinearWithThresholdFunction.FUNCTION_LINEAR_WITH_THRESHOLD.equals(function)) {
      offset = WorkUnit.create(characteristic.getPropertyValue(PROPERTY_OFFSET, null),
        characteristic.getPropertyTextValue(PROPERTY_OFFSET, null));
    }
  }

  public Rule getRule() {
    return rule;
  }

  public TechnicalDebtCharacteristic getParent() {
    return parent;
  }

  public String getRemediationFunction() {
    return function;
  }

  public WorkUnit getRemediationFactor() {
    return factor;
  }

  public WorkUnit getOffset() {
    return offset;
  }

  public org.sonar.api.qualitymodel.Characteristic toCharacteristic() {
    return characteristic;
  }
}
