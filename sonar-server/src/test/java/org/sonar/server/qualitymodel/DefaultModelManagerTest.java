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
package org.sonar.server.qualitymodel;

import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelDefinition;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DefaultModelManagerTest extends AbstractDbUnitTestCase {

  @Test
  public void reset() {
    setupData("shared");
    DefaultModelManager manager = new DefaultModelManager(getSessionFactory());

    Model model = Model.createByName("M1");
    Characteristic c1 = model.createCharacteristicByName("NEWM1C1");
    Characteristic c1a = model.createCharacteristicByName("NEWM1C1A");
    c1.addChild(c1a);

    model.createCharacteristicByName("NEWM1C2");
    manager.reset(model);

    model = getSession().getSingleResult(Model.class, "name", "M1");
    assertNotNull(model);
    assertThat(model.getCharacteristics().size(), is(3));
    assertThat(model.getCharacteristicByName("NEWM1C1A").getParents().size(), is(1));
    assertNotNull(model.getCharacteristicByName("NEWM1C1A").getParent("NEWM1C1"));
  }

  @Test
  public void noDefinitionsToRegister() {
    setupData("shared");
    ModelManager provider = new DefaultModelManager(getSessionFactory());
    provider.registerDefinitions();

    // same state
    List<Model> models = getSession().getResults(Model.class);
    assertThat(models.size(), is(2));
  }

  @Test
  public void registerOnlyNewDefinitions() {
    setupData("shared");

    ModelDefinition existingDefinition = new FakeDefinition("M1", Model.create());
    ModelDefinition newDefinition = new FakeDefinition("NEWMODEL", Model.create());

    ModelDefinition[] definitions = new ModelDefinition[]{existingDefinition, newDefinition};
    ModelManager manager = new DefaultModelManager(getSessionFactory(), definitions);
    manager.registerDefinitions();

    List<Model> models = getSession().getResults(Model.class);
    assertThat(models.size(), is(3)); // 2 existing + one new
  }

  @Test
  public void registerModelProperties() {
    Model model = Model.create();
    Characteristic characteristic = model.createCharacteristicByName("Usability");
    characteristic.setProperty("factor", 2.0);
    characteristic.setProperty("severity", "BLOCKER");

    setupData("shared");
    ModelDefinition def = new FakeDefinition("with-properties", model);
    ModelManager manager = new DefaultModelManager(getSessionFactory(), new ModelDefinition[]{def});
    manager.registerDefinitions();
    checkTables("registerModelProperties", "quality_models", "characteristics", "characteristic_properties");
  }

  @Test
  public void exists() {
    setupData("shared");
    assertTrue(DefaultModelManager.exists(getSession(), "M1"));
  }

  @Test
  public void notExists() {
    setupData("shared");
    assertFalse(DefaultModelManager.exists(getSession(), "UNKNOWN"));
  }
}

class FakeDefinition extends ModelDefinition {
  private final Model model;

  public FakeDefinition(String name, Model model) {
    super(name);
    this.model = model;
  }

  @Override
  public Model createModel() {
    return model;
  }

}