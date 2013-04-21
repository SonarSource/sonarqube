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
package org.sonar.core.qualitymodel;

import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class ModelTest extends AbstractDbUnitTestCase {

  @Test
  public void saveModelAndCharacteristics() {
    Model model = Model.createByName("fake");
    model.createCharacteristicByName("Efficiency");
    model.createCharacteristicByName("Usability");
    getSession().save(model);
    getSession().commit();

    model = getSession().getSingleResult(Model.class, "name", "fake");
    assertThat(model.getName(), is("fake"));
    assertThat(model.getCharacteristics().size(), is(2));
    assertThat(model.getRootCharacteristics().size(), is(2));
    assertNotNull(model.getCharacteristicByName("Efficiency"));
  }

  /**
   * max one parent by characteristic
   */
  @Test
  public void saveTreeOfCharacteristics() {
    Model model = Model.createByName("fake");

    Characteristic efficiency = model.createCharacteristicByName("Efficiency");
    Characteristic usability = model.createCharacteristicByName("Usability");
    Characteristic cpuEfficiency = model.createCharacteristicByName("CPU Efficiency");
    Characteristic ramEfficiency = model.createCharacteristicByName("RAM Efficiency");

    efficiency.addChildren(cpuEfficiency, ramEfficiency);

    getSession().save(model);
    getSession().commit();

    model = getSession().getSingleResult(Model.class, "name", "fake");
    assertThat(model.getCharacteristics().size(), is(4));
    assertThat(model.getCharacteristics(), hasItems(efficiency, usability, ramEfficiency, cpuEfficiency));
    assertThat(efficiency.getChildren(), hasItems(ramEfficiency, cpuEfficiency));
    assertTrue(ramEfficiency.getChildren().isEmpty());
    assertThat(ramEfficiency.getParents(), hasItems(efficiency));
  }

  /**
   * many-to-many relation between characteristics
   */
  @Test
  public void testGraphOfCharacteristics() {
    Model model = Model.createByName("fake");

    Characteristic level1a = model.createCharacteristicByName("level1a");
    Characteristic level1b = model.createCharacteristicByName("level1b");
    Characteristic level2a = model.createCharacteristicByName("level2a");
    Characteristic level2b = model.createCharacteristicByName("level2b");

    level1a.addChildren(level2a, level2b);
    level1b.addChildren(level2a, level2b);

    getSession().save(model);
    getSession().commit();

    Model persistedModel = getSession().getSingleResult(Model.class, "name", "fake");
    assertThat(persistedModel.getCharacteristics().size(), is(4));
    assertThat(persistedModel.getRootCharacteristics().size(), is(2));

    assertThat(persistedModel.getCharacteristicByName("level1a").getChildren().size(), is(2));
    assertThat(persistedModel.getCharacteristicByName("level1b").getChildren().size(), is(2));

    assertThat(persistedModel.getCharacteristicByName("level2a").getParents().size(), is(2));
    assertThat(persistedModel.getCharacteristicByName("level2b").getParents().size(), is(2));
  }
}
