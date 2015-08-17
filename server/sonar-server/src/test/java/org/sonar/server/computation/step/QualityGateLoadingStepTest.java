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

import com.google.common.base.Optional;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.qualitygate.Condition;
import org.sonar.server.computation.qualitygate.MutableQualityGateHolderRule;
import org.sonar.server.computation.qualitygate.QualityGate;
import org.sonar.server.computation.qualitygate.QualityGateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class QualityGateLoadingStepTest {
  private static final String PROJECT_KEY = "project key";
  private static final ReportComponent PROJECT_ALONE = ReportComponent.builder(Component.Type.PROJECT, 1).setKey(PROJECT_KEY).build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableQualityGateHolderRule mutableQualityGateHolder = new MutableQualityGateHolderRule();

  private SettingsRepository settingsRepository = mock(SettingsRepository.class);
  private QualityGateService qualityGateService = mock(QualityGateService.class);

  private QualityGateLoadingStep underTest = new QualityGateLoadingStep(treeRootHolder, settingsRepository, qualityGateService, mutableQualityGateHolder);

  @Test
  public void execute_sets_default_QualityGate_when_project_has_no_settings() {
    ReportComponent root = ReportComponent.builder(Component.Type.PROJECT, 1).setKey(PROJECT_KEY).addChildren(ReportComponent.builder(Component.Type.FILE, 2).build()).build();
    treeRootHolder.setRoot(root);
    when(settingsRepository.getSettings(root)).thenReturn(new Settings());

    underTest.execute();

    verifyNoQualityGate();

    // verify only project is processed
    verify(settingsRepository).getSettings(root);
    verifyNoMoreInteractions(settingsRepository);
  }

  @Test
  public void execute_sets_default_QualityGate_when_property_value_is_not_a_long() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(String.format("Unsupported value (%s) in property sonar.qualitygate", "10 sds"));

    treeRootHolder.setRoot(PROJECT_ALONE);
    when(settingsRepository.getSettings(PROJECT_ALONE)).thenReturn(new Settings().setProperty("sonar.qualitygate", "10 sds"));

    underTest.execute();
  }

  @Test
  public void execute_sets_default_QualityGate_if_it_can_not_be_found_by_service() {
    treeRootHolder.setRoot(PROJECT_ALONE);
    when(settingsRepository.getSettings(PROJECT_ALONE)).thenReturn(new Settings().setProperty("sonar.qualitygate", 10));
    when(qualityGateService.findById(10)).thenReturn(Optional.<QualityGate>absent());

    underTest.execute();

    verifyNoQualityGate();
  }

  @Test
  public void execute_sets_QualityGate_if_it_can_be_found_by_service() {
    QualityGate qualityGate = new QualityGate("name", Collections.<Condition>emptyList());

    treeRootHolder.setRoot(PROJECT_ALONE);
    when(settingsRepository.getSettings(PROJECT_ALONE)).thenReturn(new Settings().setProperty("sonar.qualitygate", 10));
    when(qualityGateService.findById(10)).thenReturn(Optional.of(qualityGate));

    underTest.execute();

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(qualityGate);
  }

  private void verifyNoQualityGate() {
    assertThat(mutableQualityGateHolder.getQualityGate()).isAbsent();
  }

}
