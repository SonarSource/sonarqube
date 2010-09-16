/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
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
}
