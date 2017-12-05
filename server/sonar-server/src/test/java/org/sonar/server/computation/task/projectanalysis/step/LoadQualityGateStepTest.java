/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.qualitygate.MutableQualityGateHolderRule;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateService;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadQualityGateStepTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public MutableQualityGateHolderRule mutableQualityGateHolder = new MutableQualityGateHolderRule();

  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private ConfigurationRepository settingsRepository = mock(ConfigurationRepository.class);
  private QualityGateService qualityGateService = mock(QualityGateService.class);

  private LoadQualityGateStep underTest = new LoadQualityGateStep(settingsRepository, qualityGateService, mutableQualityGateHolder, analysisMetadataHolder);

  @Before
  public void setUp() {
    when(analysisMetadataHolder.isShortLivingBranch()).thenReturn(false);
  }

  @Test
  public void add_hardcoded_QG_on_short_living_branch() {
    when(analysisMetadataHolder.isShortLivingBranch()).thenReturn(true);
    QualityGate qualityGate = mock(QualityGate.class);
    when(qualityGateService.findById(ShortLivingBranchQualityGate.ID)).thenReturn(Optional.of(qualityGate));

    underTest.execute();

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(qualityGate);
  }

  @Test
  public void execute_sets_default_QualityGate_when_project_has_no_settings() {
    when(settingsRepository.getConfiguration()).thenReturn(new MapSettings().asConfig());
    QualityGate defaultGate = mock(QualityGate.class);
    when(qualityGateService.findDefaultQualityGate(any())).thenReturn(defaultGate);

    underTest.execute();

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(defaultGate);
  }

  @Test
  public void execute_sets_default_QualityGate_when_property_value_is_not_a_long() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unsupported value (10 sds) in property sonar.qualitygate");

    when(settingsRepository.getConfiguration()).thenReturn(new MapSettings().setProperty("sonar.qualitygate", "10 sds").asConfig());

    underTest.execute();
  }

  @Test
  public void execute_sets_QualityGate_if_it_can_be_found_by_service() {
    QualityGate qualityGate = new QualityGate(10, "name", emptyList());

    when(settingsRepository.getConfiguration()).thenReturn(new MapSettings().setProperty("sonar.qualitygate", 10).asConfig());
    when(qualityGateService.findById(10)).thenReturn(Optional.of(qualityGate));

    underTest.execute();

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(qualityGate);
  }

}
