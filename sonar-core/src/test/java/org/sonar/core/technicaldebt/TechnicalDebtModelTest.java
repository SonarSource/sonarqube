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
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelFinder;
import org.sonar.api.rules.Rule;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TechnicalDebtModelTest {

  private Characteristic disabledCharacteristic = Characteristic.createByKey("DISABLED", "Disabled").setEnabled(false);

  @Test
  public void load_model() {
    ModelFinder modelFinder = mock(ModelFinder.class);
    Model persistedModel = createModel();
    when(modelFinder.findByName(TechnicalDebtModel.MODEL_NAME)).thenReturn(persistedModel);

    TechnicalDebtModel technicalDebtModel = new TechnicalDebtModel(modelFinder);

    assertThat(technicalDebtModel.getCharacteristics()).hasSize(2);
    assertThat(technicalDebtModel.getAllRequirements()).hasSize(1);
    assertThat(technicalDebtModel.getRequirementByRule("repo", "check1").getRule().getKey()).isEqualTo("check1");
    assertThat(technicalDebtModel.getRequirementByRule("repo", "check1").getParent().getKey()).isEqualTo("CPU_EFFICIENCY");

    // ignore disabled characteristics/requirements
    assertThat(technicalDebtModel.getCharacteristics()).excludes(disabledCharacteristic);
    assertThat(technicalDebtModel.getRequirementByRule("repo", "check2")).isNull();
  }

  private Model createModel() {
    Model model = Model.createByName(TechnicalDebtModel.MODEL_NAME);
    Characteristic efficiency = model.createCharacteristicByKey("EFFICIENCY", "Efficiency");
    model.createCharacteristicByKey("TESTABILITY", "Testability");

    Characteristic cpuEfficiency = model.createCharacteristicByKey("CPU_EFFICIENCY", "CPU Efficiency");
    efficiency.addChild(cpuEfficiency);

    Characteristic requirement1 = model.createCharacteristicByRule(Rule.create("repo", "check1", "Check One"));
    cpuEfficiency.addChild(requirement1);

    // disabled requirement
    Characteristic requirement2 = model.createCharacteristicByRule(Rule.create("repo", "check2", "Check Two"));
    requirement2.setEnabled(false);
    cpuEfficiency.addChild(requirement2);

    // disabled characteristic
    model.addCharacteristic(disabledCharacteristic);
    return model;
  }
}
