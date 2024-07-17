/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.telemetry.metrics.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.metrics.schema.BaseMessage;
import org.sonar.telemetry.metrics.schema.InstallationMetric;
import org.sonar.telemetry.metrics.schema.LanguageMetric;
import org.sonar.telemetry.metrics.schema.Metric;
import org.sonar.telemetry.metrics.schema.ProjectMetric;
import org.sonar.telemetry.metrics.schema.UserMetric;

import static org.sonar.test.JsonAssert.assertJson;

public class MessageSerializerTest {

  @ParameterizedTest
  @MethodSource("data")
  void serialize(Dimension dimension, String expectedJson) {
    BaseMessage message = getMessage(dimension);

    String json = MessageSerializer.serialize(message);

    assertJson(json).isSimilarTo(expectedJson);
  }

  @DataProvider
  public static Object[][] data() {
    return new Object[][]{
      {Dimension.INSTALLATION, expectedInstallationJson()},
      {Dimension.PROJECT, expectedProjectJson()},
      {Dimension.USER, expectedUserJson()},
      {Dimension.LANGUAGE, expectedLanguageJson()}
    };
  }

  private BaseMessage getMessage(Dimension dimension) {
    Set<Metric> metrics = null;
    switch (dimension) {
      case INSTALLATION -> metrics = installationMetrics();
      case PROJECT -> metrics = projectMetrics();
      case LANGUAGE -> metrics = languageMetrics();
      case USER -> metrics = userMetrics();
    }

    return new BaseMessage.Builder()
      .setMessageUuid("message-uuid")
      .setInstallationId("installation-id")
      .setDimension(dimension)
      .setMetrics(metrics)
      .build();
  }

  private Set<Metric> userMetrics() {
    return IntStream.range(0, 3)
      .mapToObj(i -> new UserMetric(
        "key-" + i,
        1.06f * i,
        "user-" + i,
        TelemetryDataType.FLOAT,
        Granularity.MONTHLY
      )).collect(Collectors.toSet());
  }

  private Set<Metric> languageMetrics() {
    return IntStream.range(0, 3)
      .mapToObj(i -> new LanguageMetric(
        "key-" + i,
        i % 2 == 0,
        "java",
        TelemetryDataType.BOOLEAN,
        Granularity.MONTHLY
      )).collect(Collectors.toSet());
  }

  private Set<Metric> projectMetrics() {
    return IntStream.range(0, 3)
      .mapToObj(i -> new ProjectMetric(
        "key-" + i,
        "value-" + i,
        "project-" + i,
        TelemetryDataType.STRING,
        Granularity.WEEKLY
      )).collect(Collectors.toSet());
  }

  private Set<Metric> installationMetrics() {
    return IntStream.range(0, 3)
      .mapToObj(i -> new InstallationMetric(
        "key-" + i,
        i,
        TelemetryDataType.INTEGER,
        Granularity.DAILY
      )).collect(Collectors.toSet());
  }

  private static String expectedInstallationJson() {
    return """
    {
      "message_uuid": "message-uuid",
      "installation_id": "installation-id",
      "dimension": "installation",
      "metric_values": [
        {
          "key": "key-0",
          "value": 0,
          "type": "integer",
          "granularity": "daily"
        },
        {
          "key": "key-2",
          "value": 2,
          "type": "integer",
          "granularity": "daily"
        },
        {
          "key": "key-1",
          "value": 1,
          "type": "integer",
          "granularity": "daily"
        }
      ]
    }""";
  }

  private static String expectedProjectJson() {
    return """
    {
      "message_uuid": "message-uuid",
      "installation_id": "installation-id",
      "dimension": "project",
      "metric_values": [
        {
          "key": "key-0",
          "value": "value-0",
          "type": "string",
          "granularity": "weekly",
          "project_uuid": "project-0"
        },
        {
          "key": "key-1",
          "value": "value-1",
          "type": "string",
          "granularity": "weekly",
          "project_uuid": "project-1"
        },
        {
          "key": "key-2",
          "value": "value-2",
          "type": "string",
          "granularity": "weekly",
          "project_uuid": "project-2"
        }
      ]
    }""";
  }

  private static String expectedUserJson() {
    return """
    {
      "message_uuid": "message-uuid",
      "installation_id": "installation-id",
      "dimension": "user",
      "metric_values": [
        {
          "key": "key-0",
          "value": 0.0,
          "type": "float",
          "granularity": "monthly",
          "user_uuid": "user-0"
        },
        {
          "key": "key-1",
          "value": 1.06,
          "type": "float",
          "granularity": "monthly",
          "user_uuid": "user-1"
        },
        {
          "key": "key-2",
          "value": 2.12,
          "type": "float",
          "granularity": "monthly",
          "user_uuid": "user-2"
        }
      ]
    }""";
  }

  private static String expectedLanguageJson() {
    return """
    {
      "message_uuid": "message-uuid",
      "installation_id": "installation-id",
      "dimension": "language",
      "metric_values": [
        {
          "key": "key-0",
          "value": true,
          "type": "boolean",
          "granularity": "monthly",
          "language": "java"
        },
        {
          "key": "key-2",
          "value": true,
          "type": "boolean",
          "granularity": "monthly",
          "language": "java"
        },
        {
          "key": "key-1",
          "value": false,
          "type": "boolean",
          "granularity": "monthly",
          "language": "java"
        }
      ]
    }""";
  }

}
