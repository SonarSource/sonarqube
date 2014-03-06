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
package org.sonar.batch.qualitygate;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.core.timemachine.Periods;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualityGateVerifierTest {

  QualityGateVerifier verifier;
  DecoratorContext context;
  QualityGate qualityGate;

  Measure measureClasses;
  Measure measureCoverage;
  Measure measureComplexity;
  Resource project;
  Snapshot snapshot;
  Periods periods;
  I18n i18n;

  @Before
  public void before() {
    context = mock(DecoratorContext.class);
    periods = mock(Periods.class);
    i18n = mock(I18n.class);
    when(i18n.message(any(Locale.class), eq("variation"), eq("variation"))).thenReturn("variation");

    measureClasses = new Measure(CoreMetrics.CLASSES, 20d);
    measureCoverage = new Measure(CoreMetrics.COVERAGE, 35d);
    measureComplexity = new Measure(CoreMetrics.COMPLEXITY, 50d);

    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(measureClasses);
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(measureCoverage);
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(measureComplexity);

    snapshot = mock(Snapshot.class);
    qualityGate = mock(QualityGate.class);
    when(qualityGate.isEnabled()).thenReturn(true);
    verifier = new QualityGateVerifier(qualityGate);
    project = new Project("foo");
  }

  @Test
  public void should_be_executed_if_quality_gate_is_enabled() throws Exception {
    assertThat(verifier.shouldExecuteOnProject((Project) project)).isTrue();
    when(qualityGate.isEnabled()).thenReturn(false);
    assertThat(verifier.shouldExecuteOnProject((Project) project)).isFalse();
  }

  @Test
  public void test_toString() {
    assertThat(verifier.toString()).isEqualTo("QualityGateVerifier");
  }

  @Test
  public void generates_quality_gates_status() {
    assertThat(verifier.generatesQualityGateStatus()).isEqualTo(CoreMetrics.QUALITY_GATE_STATUS);
  }

  @Test
  public void depends_on_variations() {
    assertThat(verifier.dependsOnVariations()).isEqualTo(DecoratorBarriers.END_OF_TIME_MACHINE);
  }

}
