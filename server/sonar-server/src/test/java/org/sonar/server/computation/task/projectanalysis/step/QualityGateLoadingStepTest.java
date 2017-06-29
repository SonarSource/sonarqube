/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Optional;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.VisitException;
import org.sonar.server.computation.task.projectanalysis.qualitygate.Condition;
import org.sonar.server.computation.task.projectanalysis.qualitygate.MutableQualityGateHolderRule;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateService;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.test.ExceptionCauseMatcher.hasType;

public class QualityGateLoadingStepTest {
  private static final String PROJECT_KEY = "project key";
  private static final ReportComponent PROJECT_ALONE = ReportComponent.builder(Component.Type.PROJECT, 1).setKey(PROJECT_KEY).build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableQualityGateHolderRule mutableQualityGateHolder = new MutableQualityGateHolderRule();

  private ConfigurationRepository settingsRepository = mock(ConfigurationRepository.class);
  private QualityGateService qualityGateService = mock(QualityGateService.class);

  private LoadQualityGateStep underTest = new LoadQualityGateStep(treeRootHolder, settingsRepository, qualityGateService, mutableQualityGateHolder);

  @Test
  public void execute_sets_default_QualityGate_when_project_has_no_settings() {
    ReportComponent root = ReportComponent.builder(Component.Type.PROJECT, 1).setKey(PROJECT_KEY).addChildren(ReportComponent.builder(Component.Type.FILE, 2).build()).build();
    treeRootHolder.setRoot(root);
    when(settingsRepository.getConfiguration(root)).thenReturn(new MapSettings().asConfig());

    underTest.execute();

    verifyNoQualityGate();

    // verify only project is processed
    verify(settingsRepository).getConfiguration(root);
    verifyNoMoreInteractions(settingsRepository);
  }

  @Test
  public void execute_sets_default_QualityGate_when_property_value_is_not_a_long() {
    expectedException.expect(VisitException.class);
    expectedException.expectCause(
      hasType(IllegalStateException.class)
        .andMessage(format("Unsupported value (%s) in property sonar.qualitygate", "10 sds")));

    treeRootHolder.setRoot(PROJECT_ALONE);
    when(settingsRepository.getConfiguration(PROJECT_ALONE)).thenReturn(new MapSettings().setProperty("sonar.qualitygate", "10 sds").asConfig());

    underTest.execute();
  }

  @Test
  public void execute_sets_default_QualityGate_if_it_can_not_be_found_by_service() {
    treeRootHolder.setRoot(PROJECT_ALONE);
    when(settingsRepository.getConfiguration(PROJECT_ALONE)).thenReturn(new MapSettings().setProperty("sonar.qualitygate", 10).asConfig());
    when(qualityGateService.findById(10)).thenReturn(Optional.<QualityGate>absent());

    underTest.execute();

    verifyNoQualityGate();
  }

  @Test
  public void execute_sets_QualityGate_if_it_can_be_found_by_service() {
    QualityGate qualityGate = new QualityGate(465, "name", Collections.<Condition>emptyList());

    treeRootHolder.setRoot(PROJECT_ALONE);
    when(settingsRepository.getConfiguration(PROJECT_ALONE)).thenReturn(new MapSettings().setProperty("sonar.qualitygate", 10).asConfig());
    when(qualityGateService.findById(10)).thenReturn(Optional.of(qualityGate));

    underTest.execute();

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(qualityGate);
  }

  private void verifyNoQualityGate() {
    assertThat(mutableQualityGateHolder.getQualityGate()).isAbsent();
  }

}
