/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.computation.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.formula.CoreFormulaRepository;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CoreMetricFormulaExecutorStepTest {
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Test
  public void verify_execute_formulas_from_CoreFormulaRepository() {
    CoreFormulaRepository coreFormulaRepository = mock(CoreFormulaRepository.class);
    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.PROJECT, 1).build());

    CoreMetricFormulaExecutorStep underTest = new CoreMetricFormulaExecutorStep(treeRootHolder,
      mock(MetricRepository.class), mock(MeasureRepository.class), coreFormulaRepository);

    underTest.execute();

    verify(coreFormulaRepository, times(1)).getFormulas();
  }
}
