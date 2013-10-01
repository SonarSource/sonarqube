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
import org.sonar.core.technicaldebt.functions.ConstantFunction;
import org.sonar.core.technicaldebt.functions.LinearFunction;
import org.sonar.core.technicaldebt.functions.LinearWithOffsetFunction;
import org.sonar.core.technicaldebt.functions.LinearWithThresholdFunction;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TechnicalDebtRequirementTest {

  @Test
  public void defaultFactor() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFactor().getValue(), is(WorkUnit.DEFAULT_VALUE));
    assertThat(requirement.getRemediationFactor().getUnit(), is(WorkUnit.DEFAULT_UNIT));
  }

  @Test
  public void testOverriddenFactor() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FACTOR, 3.14);
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFactor().getValue(), is(3.14));
    assertThat(requirement.getRemediationFactor().getUnit(), is(WorkUnit.DAYS));
  }

  @Test
  public void defaultFunctionIsLinear() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction(), is(LinearFunction.FUNCTION_LINEAR));
    assertThat(requirement.getOffset(), is(nullValue()));
  }

  @Test
  public void testOverriddenFunction() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, ConstantFunction.FUNCTION_CONSTANT_RESOURCE);
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction(), is(ConstantFunction.FUNCTION_CONSTANT_RESOURCE));
  }

  @Test
  public void testDefaultLinearWithOffset() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, LinearWithOffsetFunction.FUNCTION_LINEAR_WITH_OFFSET);
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction(), is(LinearWithOffsetFunction.FUNCTION_LINEAR_WITH_OFFSET));
    assertThat(requirement.getRemediationFactor().getValue(), is(WorkUnit.DEFAULT_VALUE));
    assertThat(requirement.getRemediationFactor().getUnit(), is(WorkUnit.DEFAULT_UNIT));
    assertThat(requirement.getOffset().getValue(), is(WorkUnit.DEFAULT_VALUE));
    assertThat(requirement.getOffset().getUnit(), is(WorkUnit.DEFAULT_UNIT));
  }

  @Test
  public void testCustomizedLinearWithOffset() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, LinearWithOffsetFunction.FUNCTION_LINEAR_WITH_OFFSET);
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_OFFSET, 5.0);
    persistedRequirement.addProperty(persistedRequirement.getProperty(TechnicalDebtRequirement.PROPERTY_OFFSET).setTextValue("h"));
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction(), is(LinearWithOffsetFunction.FUNCTION_LINEAR_WITH_OFFSET));
    assertThat(requirement.getOffset().getValue(), is(5.0));
    assertThat(requirement.getOffset().getUnit(), is(WorkUnit.HOURS));
  }

  @Test
  public void testDefaultLinearWithThreshold() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, LinearWithThresholdFunction.FUNCTION_LINEAR_WITH_THRESHOLD);
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction(), is(LinearWithThresholdFunction.FUNCTION_LINEAR_WITH_THRESHOLD));
    assertThat(requirement.getRemediationFactor().getValue(), is(WorkUnit.DEFAULT_VALUE));
    assertThat(requirement.getRemediationFactor().getUnit(), is(WorkUnit.DEFAULT_UNIT));
    assertThat(requirement.getOffset().getValue(), is(WorkUnit.DEFAULT_VALUE));
    assertThat(requirement.getOffset().getUnit(), is(WorkUnit.DEFAULT_UNIT));
  }

  @Test
  public void testCustomizedLinearWithThreshold() {
    Characteristic persistedRequirement = Characteristic.createByName("Efficiency");
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_REMEDIATION_FUNCTION, LinearWithThresholdFunction.FUNCTION_LINEAR_WITH_THRESHOLD);
    persistedRequirement.setProperty(TechnicalDebtRequirement.PROPERTY_OFFSET, 5.0);
    persistedRequirement.addProperty(persistedRequirement.getProperty(TechnicalDebtRequirement.PROPERTY_OFFSET).setTextValue("h"));
    TechnicalDebtRequirement requirement = new TechnicalDebtRequirement(persistedRequirement, null);
    assertThat(requirement.getRemediationFunction(), is(LinearWithThresholdFunction.FUNCTION_LINEAR_WITH_THRESHOLD));
    assertThat(requirement.getOffset().getValue(), is(5.0));
    assertThat(requirement.getOffset().getUnit(), is(WorkUnit.HOURS));
  }
}
