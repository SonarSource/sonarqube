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

import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;

import static org.fest.assertions.Assertions.assertThat;

public class TechnicalDebtRequirementTest {

  @Test
  public void default_factor() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFactor().getValue()).isEqualTo(WorkUnit.DEFAULT_VALUE);
    assertThat(requirement.getRemediationFactor().getUnit()).isEqualTo(WorkUnit.DEFAULT_UNIT);
  }

  @Test
  public void overridde_factor() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FACTOR, 3.14);

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFactor().getValue()).isEqualTo(3.14);
    assertThat(requirement.getRemediationFactor().getUnit()).isEqualTo(WorkUnit.DAYS);
  }

  @Test
  public void default_function_is_linear() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction()).isEqualTo(TechnicalDebtRequirement.FUNCTION_LINEAR);
    assertThat(requirement.getOffset()).isNull();
  }

  @Test
  public void linear() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, TechnicalDebtRequirement.FUNCTION_LINEAR);
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FACTOR, 3.14);

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction()).isEqualTo(TechnicalDebtRequirement.FUNCTION_LINEAR);
    assertThat(requirement.getRemediationFactor().getValue()).isEqualTo(3.14);
    assertThat(requirement.getRemediationFactor().getUnit()).isEqualTo(WorkUnit.DAYS);
    assertThat(requirement.getOffset()).isNull();
  }

  @Test
  public void default_linear_with_offset() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, TechnicalDebtRequirement.FUNCTION_LINEAR_WITH_OFFSET);

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction()).isEqualTo(TechnicalDebtRequirement.FUNCTION_LINEAR_WITH_OFFSET);
    assertThat(requirement.getRemediationFactor().getValue()).isEqualTo(WorkUnit.DEFAULT_VALUE);
    assertThat(requirement.getRemediationFactor().getUnit()).isEqualTo(WorkUnit.DEFAULT_UNIT);
    assertThat(requirement.getOffset().getValue()).isEqualTo(WorkUnit.DEFAULT_VALUE);
    assertThat(requirement.getOffset().getUnit()).isEqualTo(WorkUnit.DEFAULT_UNIT);
  }

  @Test
  public void linear_with_offset() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, TechnicalDebtRequirement.FUNCTION_LINEAR_WITH_OFFSET);
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_OFFSET, 5.0);
    persistedRequirement.addProperty(persistedRequirement.getProperty(TechnicalDebtRequirement.PROPERTY_OFFSET).setTextValue("h"));

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction()).isEqualTo(TechnicalDebtRequirement.FUNCTION_LINEAR_WITH_OFFSET);
    assertThat(requirement.getOffset().getValue()).isEqualTo(5.0);
    assertThat(requirement.getOffset().getUnit()).isEqualTo(WorkUnit.HOURS);
  }

  @Test
  public void constant_per_issue() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, TechnicalDebtRequirement.FUNCTION_CONSTANT_PER_ISSUE);
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_OFFSET, 5.0);
    persistedRequirement.addProperty(persistedRequirement.getProperty(TechnicalDebtRequirement.PROPERTY_OFFSET).setTextValue("h"));

    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction()).isEqualTo(TechnicalDebtRequirement.FUNCTION_CONSTANT_PER_ISSUE);
    assertThat(requirement.getRemediationFactor()).isNull();
    assertThat(requirement.getOffset().getValue()).isEqualTo(5.0);
    assertThat(requirement.getOffset().getUnit()).isEqualTo(WorkUnit.HOURS);
  }

}
