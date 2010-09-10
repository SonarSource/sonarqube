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
package org.sonar.core.qualitymodel;

import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelDefinition;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DefaultModelFinderTest extends AbstractDbUnitTestCase {

  @Test
  public void reset() {
    setupData("shared");
    DefaultModelFinder provider = new DefaultModelFinder(getSessionFactory());

    Model model = Model.createByName("M1");
    Characteristic c1 = model.createCharacteristicByName("NEWM1C1");
    Characteristic c1a = model.createCharacteristicByName("NEWM1C1A");
    c1.addChild(c1a);

    model.createCharacteristicByName("NEWM1C2");
    model = provider.reset(model);

    model = getSession().getSingleResult(Model.class, "name", "M1");
    assertNotNull(model);
    assertThat(model.getCharacteristics().size(), is(3));
    assertThat(model.getCharacteristicByName("NEWM1C1A").getParents().size(), is(1));
    assertNotNull(model.getCharacteristicByName("NEWM1C1A").getParent("NEWM1C1"));
  }

  @Test
  public void findByName() {
    setupData("shared");
    DefaultModelFinder provider = new DefaultModelFinder(getSessionFactory());
    Model model = provider.findByName("M1");
    assertNotNull(model);
    assertNotNull(model.getCharacteristicByName("M1C1"));
  }

  @Test
  public void findByNameNotFound() {
    setupData("shared");
    DefaultModelFinder provider = new DefaultModelFinder(getSessionFactory());
    assertNull(provider.findByName("UNKNOWN"));
  }

  @Test
  public void noDefinitionsToRegister() {
    setupData("shared");
    DefaultModelFinder provider = new DefaultModelFinder(getSessionFactory());
    provider.registerDefinitions();

    // same state
    List<Model> models = getSession().getResults(Model.class);
    assertThat(models.size(), is(2));
  }

  @Test
  public void registerOnlyNewDefinitions() {
    setupData("shared");

    ModelDefinition existingDefinition = new FakeDefinition("M1");
    ModelDefinition newDefinition = new FakeDefinition("NEWMODEL");

    ModelDefinition[] definitions = new ModelDefinition[]{existingDefinition, newDefinition};
    DefaultModelFinder provider = new DefaultModelFinder(getSessionFactory(), definitions);
    provider.registerDefinitions();

    List<Model> models = getSession().getResults(Model.class);
    assertThat(models.size(), is(3)); // 2 existing + one new
  }

  @Test
  public void exists() {
    setupData("shared");
    assertTrue(DefaultModelFinder.exists(getSession(), "M1"));
  }

  @Test
  public void notExists() {
    setupData("shared");
    assertFalse(DefaultModelFinder.exists(getSession(), "UNKNOWN"));
  }
}

class FakeDefinition extends ModelDefinition {

  public FakeDefinition(String name) {
    super(name);
  }

  @Override
  public Model create() {
    return Model.create();
  }

}