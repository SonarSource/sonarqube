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

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DefaultRequirementTest {

  @Test
  public void add_factor_property() {
    DefaultRequirementProperty property = new DefaultRequirementProperty()
      .setKey("remediationFactor")
      .setValue(30d)
      .setTextValue("mn");

    DefaultRequirement requirement = new DefaultRequirement();
    requirement.addProperty(property);

    assertThat(requirement.factor()).isNotNull();
    assertThat(requirement.factor().getUnit()).isEqualTo("mn");
    assertThat(requirement.factor().getValue()).isEqualTo(30d);
  }

  @Test
  public void add_offset_property() {
    DefaultRequirementProperty property = new DefaultRequirementProperty()
      .setKey("offset")
      .setValue(30d)
      .setTextValue("mn");

    DefaultRequirement requirement = new DefaultRequirement();
    requirement.addProperty(property);

    assertThat(requirement.offset()).isNotNull();
    assertThat(requirement.offset().getValue()).isEqualTo(30d);
    assertThat(requirement.offset().getUnit()).isEqualTo("mn");
  }

  @Test
  public void add_linear_function_property() {
    DefaultRequirementProperty property = new DefaultRequirementProperty()
      .setKey("remediationFunction")
      .setTextValue("linear");

    DefaultRequirement requirement = new DefaultRequirement();
    requirement.addProperty(property);

    assertThat(requirement.function()).isEqualTo("linear");
  }

  @Test
  public void add_constant_function_property() {
    DefaultRequirementProperty property = new DefaultRequirementProperty()
      .setKey("remediationFunction")
      .setTextValue("constant_resource");

    DefaultRequirement requirement = new DefaultRequirement();
    requirement.addProperty(property);

    assertThat(requirement.function()).isEqualTo("constant_resource");
    assertThat(requirement.factor()).isNull();
    assertThat(requirement.offset()).isNull();
  }

  @Test
  public void fail_on_invalid_function() {
    DefaultRequirementProperty property = new DefaultRequirementProperty()
      .setKey("remediationFunction")
      .setTextValue("unknown");

    DefaultRequirement requirement = new DefaultRequirement();
    try {
      requirement.addProperty(property);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Function is not valid. Should be one of : [constant_resource, linear, linear_offset, linear_threshold]");
    }
  }

  @Test
  public void fail_on_invalid_property_key() {
    DefaultRequirementProperty property = new DefaultRequirementProperty()
      .setKey("unknown");

    DefaultRequirement requirement = new DefaultRequirement();
    try {
      requirement.addProperty(property);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Property key is not found");
    }
  }

}
