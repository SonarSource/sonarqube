/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnboardingSystemInfoSectionTest {

  private final TelemetryOnboardingCountsProvider countsProvider = mock(TelemetryOnboardingCountsProvider.class);
  private final TelemetryOnboardingBoundProjectsByAlmProvider boundProjectsByAlmProvider = mock(TelemetryOnboardingBoundProjectsByAlmProvider.class);
  private final TelemetryOnboardingLastAnalysisBucketProvider lastAnalysisBucketProvider = mock(TelemetryOnboardingLastAnalysisBucketProvider.class);

  private final OnboardingSystemInfoSection underTest = new OnboardingSystemInfoSection(
    countsProvider, boundProjectsByAlmProvider, lastAnalysisBucketProvider);

  @Test
  void toProtobuf_shouldReturnSectionNamedOnboarding() {
    stubEmptyValues();

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getName()).isEqualTo("Onboarding");
  }

  @Test
  void toProtobuf_whenNoData_shouldReportEveryAttributeAtZero() {
    stubEmptyValues();

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList())
      .extracting(Attribute::getLongValue)
      .containsOnly(0L);
    assertThat(section.getAttributesList()).hasSize(16);
  }

  @Test
  void toProtobuf_shouldExposeCountsBoundByAlmAndBucketsAsLongAttributes() {
    when(countsProvider.getValues()).thenReturn(Map.of(
      TelemetryOnboardingCountsProvider.KEY_TOTAL_PROJECTS, 10,
      TelemetryOnboardingCountsProvider.KEY_ANALYSED_PROJECTS, 7,
      TelemetryOnboardingCountsProvider.KEY_ALM_IMPORTED_PROJECTS, 4,
      TelemetryOnboardingCountsProvider.KEY_CONFIGURED_ALM, 2));
    when(boundProjectsByAlmProvider.getValues()).thenReturn(Map.of(
      "github", 3,
      TelemetryOnboardingBoundProjectsByAlmProvider.KEY_NOT_BOUND, 5,
      TelemetryOnboardingBoundProjectsByAlmProvider.KEY_NOT_BOUND_SCANNED, 2));
    when(lastAnalysisBucketProvider.getValues()).thenReturn(Map.of(
      TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_LE_7D, 1,
      TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_NEVER, 3));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getAttributesList())
      .extracting(Attribute::getKey, Attribute::getLongValue)
      .contains(
        tuple("Total Projects", 10L),
        tuple("Analysed Projects", 7L),
        tuple("ALM Imported Projects", 4L),
        tuple("Configured ALM Integrations", 2L),
        tuple("Bound Projects (github)", 3L),
        tuple("Bound Projects (gitlab)", 0L),
        tuple("Not Bound Projects", 5L),
        tuple("Not Bound but Analysed Projects", 2L),
        tuple("Projects Analysed - Last 7 Days", 1L),
        tuple("Projects Analysed - Last 30 Days", 0L),
        tuple("Projects Never Analysed", 3L));
  }

  private void stubEmptyValues() {
    when(countsProvider.getValues()).thenReturn(Map.of());
    when(boundProjectsByAlmProvider.getValues()).thenReturn(Map.of());
    when(lastAnalysisBucketProvider.getValues()).thenReturn(Map.of());
  }
}
