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

import javax.annotation.CheckForNull;

public class TechnicalDebtRequirement {

  public static final String PROPERTY_REMEDIATION_FUNCTION = "remediationFunction";
  public static final String PROPERTY_REMEDIATION_FACTOR = "remediationFactor";
  public static final String PROPERTY_OFFSET = "offset";

  public static final String FUNCTION_LINEAR = "linear";
  public static final String FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";
  public static final String FUNCTION_CONSTANT_PER_ISSUE = "constant_issue";

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
    function = characteristic.getPropertyTextValue(PROPERTY_REMEDIATION_FUNCTION, FUNCTION_LINEAR);
  }

  private void initFactor() {
    if (FUNCTION_LINEAR.equals(function) || FUNCTION_LINEAR_WITH_OFFSET.equals(function)) {
      factor = WorkUnit.create(characteristic.getPropertyValue(PROPERTY_REMEDIATION_FACTOR, null), characteristic.getPropertyTextValue(PROPERTY_REMEDIATION_FACTOR, null));
    }
  }

  private void initOffset() {
    if (FUNCTION_LINEAR_WITH_OFFSET.equals(function) || FUNCTION_CONSTANT_PER_ISSUE.equals(function)) {
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

  @CheckForNull
  public WorkUnit getRemediationFactor() {
    return factor;
  }

  @CheckForNull
  public WorkUnit getOffset() {
    return offset;
  }

  public Characteristic toCharacteristic() {
    return characteristic;
  }
}
