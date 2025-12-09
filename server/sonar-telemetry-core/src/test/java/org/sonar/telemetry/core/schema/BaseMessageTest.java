/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.telemetry.core.schema;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseMessageTest {

  @Test
  void build() {
    BaseMessage message = new BaseMessage.Builder()
      .setMessageUuid("123e4567-e89b-12d3-a456-426614174000")
      .setInstallationId("installation-id")
      .setDimension(Dimension.INSTALLATION)
      .setMetrics(installationMetrics())
      .build();

    assertThat(message.getMessageUuid()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    assertThat(message.getInstallationId()).isEqualTo("installation-id");
    assertThat(message.getDimension()).isEqualTo(Dimension.INSTALLATION);
    Set<InstallationMetric> installationMetrics = (Set<InstallationMetric>) (Set<?>) message.getMetrics();
    assertThat(installationMetrics)
      .extracting(InstallationMetric::getKey, InstallationMetric::getGranularity, InstallationMetric::getType, InstallationMetric::getValue)
      .containsExactlyInAnyOrder(
        tuple("key-0", Granularity.DAILY, TelemetryDataType.INTEGER, 0),
        tuple("key-1", Granularity.DAILY, TelemetryDataType.INTEGER, 1),
        tuple("key-2", Granularity.DAILY, TelemetryDataType.INTEGER, 2)
      );
  }

  @ParameterizedTest
  @MethodSource("invalidBaseMessageProvider")
  void build_invalidCases(BaseMessage.Builder builder, String expectedErrorMessage) {
    Exception exception = assertThrows(NullPointerException.class, builder::build);
    assertEquals(expectedErrorMessage, exception.getMessage());
  }

  private static Stream<Arguments> invalidBaseMessageProvider() {
    return Stream.of(
      Arguments.of(
        new BaseMessage.Builder()
          .setInstallationId("installation-id")
          .setDimension(Dimension.INSTALLATION)
          .setMetrics(installationMetrics()),
        "messageUuid must be specified"
      ),
      Arguments.of(
        new BaseMessage.Builder()
          .setMessageUuid("some-uuid")
          .setInstallationId("installation-id")
          .setMetrics(installationMetrics()),
        "dimension must be specified"
      ),
      Arguments.of(
        new BaseMessage.Builder()
          .setMessageUuid("some-uuid")
          .setDimension(Dimension.INSTALLATION)
          .setMetrics(installationMetrics()),
        "installationId must be specified"
      )
    );
  }

  private static Set<Metric> installationMetrics() {
    return IntStream.range(0, 3)
      .mapToObj(i -> new InstallationMetric(
        "key-" + i,
        i,
        TelemetryDataType.INTEGER,
        Granularity.DAILY
      )).collect(Collectors.toSet());
  }
}
