/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.qualitymodel;

import org.junit.Test;
import org.sonar.api.rules.Rule;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ModelTest {
  @Test
  public void searchEnabledCharacteristics() {
    Model model = Model.create();
    model.createCharacteristicByKey("foo", "enabled foo");
    model.createCharacteristicByKey("foo", "disabled foo").setEnabled(false);

    assertThat(model.getCharacteristicByKey("foo").getName(), is("enabled foo"));
    assertThat(model.getCharacteristicByKey("foo").getEnabled(), is(true));

    assertThat(model.getCharacteristicByName("enabled foo").getName(), is("enabled foo"));
    assertThat(model.getCharacteristicByName("disabled foo"), nullValue());

    assertThat(model.getCharacteristics().size(), is(1));
    assertThat(model.getCharacteristics(false).size(), is(2));
  }

  @Test
  public void shouldFindCharacteristicByRule() {
    Model model = Model.create();
    Rule rule1 = Rule.create("checkstyle", "regexp", "Regular expression");
    Rule rule2 = Rule.create("checkstyle", "import", "Check imports");

    Characteristic efficiency = model.createCharacteristicByName("Efficiency");
    Characteristic requirement1 = model.createCharacteristicByRule(rule1);
    Characteristic requirement2 = model.createCharacteristicByRule(rule2);
    efficiency.addChild(requirement1);
    efficiency.addChild(requirement2);

    assertThat(model.getCharacteristicByRule(rule1), is(requirement1));
    assertThat(model.getCharacteristicByRule(rule2), is(requirement2));
    assertThat(model.getCharacteristicByRule(null), nullValue());
    assertThat(model.getCharacteristicByRule(Rule.create("foo", "bar", "Bar")), nullValue());
  }

  @Test
  public void shouldRemoveCharacteristic() {
    Model model = Model.create();
    Characteristic efficiency = model.createCharacteristicByName("Efficiency");
    model.createCharacteristicByName("Usability");
    assertThat(model.getCharacteristics().size(), is(2));

    model.removeCharacteristic(efficiency);
    assertThat(model.getCharacteristics().size(), is(1));
    assertThat(model.getCharacteristicByName("Efficiency"), nullValue());
    assertThat(model.getCharacteristicByName("Usability"), notNullValue());
  }

  @Test
  public void shouldNotFailWhenRemovingUnknownCharacteristic() {
    Model model = Model.create();
    model.createCharacteristicByName("Efficiency");
    model.removeCharacteristic(Characteristic.createByKey("foo", "Foo"));
    assertThat(model.getCharacteristics().size(), is(1));
  }
}
