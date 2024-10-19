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
package org.sonar.telemetry.metrics;

import com.tngtech.java.junit.dataprovider.DataProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.telemetry.TelemetryMetricsSentDto;
import org.sonar.telemetry.FakeServer;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.metrics.schema.BaseMessage;
import org.sonar.telemetry.metrics.schema.InstallationMetric;
import org.sonar.telemetry.metrics.schema.LanguageMetric;
import org.sonar.telemetry.metrics.schema.ProjectMetric;
import org.sonar.telemetry.metrics.schema.UserMetric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryMetricsLoaderIT {

  private static final String SOME_UUID = "some-uuid";
  private static final Long NOW = 100_000_000L;
  private static final String SERVER_ID = "AU-TpxcB-iU5OvuD2FL7";
  public static final String KEY_IS_VALID = "is_valid";
  public static final String KEY_COVERAGE = "coverage";
  public static final String KEY_NCLOC = "ncloc";
  public static final String DIMENSION_INSTALLATION = "installation";
  public static final String DIMENSION_PROJECT = "project";
  public static final String DIMENSION_LANGUAGE = "language";
  public static final String KEY_LAST_ACTIVE = "last_active";
  public static final String KEY_UPDATE_FINISHED = "update_finished";
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  public DbTester db = DbTester.create(system2);
  private final FakeServer server = new FakeServer();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final List<TelemetryDataProvider<?>> providers = List.of(
    getBean(KEY_IS_VALID, Dimension.INSTALLATION, Granularity.DAILY, false),
    getBean(KEY_COVERAGE, Dimension.PROJECT, Granularity.WEEKLY, 12.05F, "module-1", "module-2"),
    getBean(KEY_NCLOC, Dimension.LANGUAGE, Granularity.MONTHLY, 125, "java", "cpp"),
    getBean(KEY_LAST_ACTIVE, Dimension.USER, Granularity.DAILY, "2024-01-01", "user-1"),
    getBean(KEY_UPDATE_FINISHED, Dimension.INSTALLATION, Granularity.ADHOC, true),
    getBean(KEY_UPDATE_FINISHED, Dimension.INSTALLATION, Granularity.ADHOC, false)
  );
  private final TelemetryMetricsLoader underTest = new TelemetryMetricsLoader(system2, server, db.getDbClient(), uuidFactory, providers);

  @Test
  void sendTelemetryData() {
    when(uuidFactory.create()).thenReturn(SOME_UUID);

    server.setId(SERVER_ID);
    TelemetryMetricsLoader.Context context = underTest.loadData();
    Set<BaseMessage> messages = context.getMessages();

    assertThat(context.getMetricsToUpdate())
      .hasSize(6)
      .extracting(TelemetryMetricsSentDto::getMetricKey, TelemetryMetricsSentDto::getDimension, TelemetryMetricsSentDto::getLastSent)
      .containsExactlyInAnyOrder(
        tuple(KEY_IS_VALID, DIMENSION_INSTALLATION, 0L),
        tuple(KEY_COVERAGE, DIMENSION_PROJECT, 0L),
        tuple(KEY_NCLOC, DIMENSION_LANGUAGE, 0L),
        tuple(KEY_LAST_ACTIVE, "user", 0L),
        tuple(KEY_UPDATE_FINISHED, DIMENSION_INSTALLATION, 0L),
        tuple(KEY_UPDATE_FINISHED, DIMENSION_INSTALLATION, 0L)
      );

    assertThat(messages)
      .hasSize(4)
      .extracting(BaseMessage::getMessageUuid, BaseMessage::getInstallationId, BaseMessage::getDimension)
      .containsExactlyInAnyOrder(
        tuple(SOME_UUID, SERVER_ID, Dimension.INSTALLATION),
        tuple(SOME_UUID, SERVER_ID, Dimension.USER),
        tuple(SOME_UUID, SERVER_ID, Dimension.PROJECT),
        tuple(SOME_UUID, SERVER_ID, Dimension.LANGUAGE)
      );

    messages.forEach(message -> {
      switch (message.getDimension()) {
        case INSTALLATION -> assertInstallationMetrics(message);
        case USER -> assertUserMetrics(message);
        case LANGUAGE -> assertLanguageMetrics(message);
        case PROJECT -> assertProjectMetrics(message);
        default -> throw new IllegalArgumentException("Should not get here");
      }
    });
  }

  @ParameterizedTest
  @MethodSource("shouldNotBeUpdatedMetrics")
  void loadData_whenDailyMetricsShouldNotBeSent(String key, String dimension, TimeUnit unit, int offset) {
    when(uuidFactory.create()).thenReturn(SOME_UUID);

    server.setId(SERVER_ID);

    system2.setNow(1L);
    TelemetryMetricsSentDto dto = new TelemetryMetricsSentDto(key, dimension);
    db.getDbClient().telemetryMetricsSentDao().upsert(db.getSession(), dto);
    db.commit();

    system2.setNow(unit.toMillis(offset));
    TelemetryMetricsLoader.Context context = underTest.loadData();

    List<TelemetryMetricsSentDto> toUpdate = context.getMetricsToUpdate();

    assertThat(toUpdate)
      .hasSize(5)
      .extracting(TelemetryMetricsSentDto::getMetricKey)
      .doesNotContain(key);
  }

  @ParameterizedTest
  @MethodSource("shouldBeUpdatedMetrics")
  void loadData_whenDailyMetricsShouldBeSent(String key, String dimension, TimeUnit unit, int offset) {
    when(uuidFactory.create()).thenReturn(SOME_UUID);

    server.setId(SERVER_ID);

    system2.setNow(1L);
    TelemetryMetricsSentDto dto = new TelemetryMetricsSentDto(key, dimension);
    db.getDbClient().telemetryMetricsSentDao().upsert(db.getSession(), dto);
    db.commit();

    system2.setNow(unit.toMillis(offset));
    TelemetryMetricsLoader.Context context = underTest.loadData();

    List<TelemetryMetricsSentDto> toUpdate = context.getMetricsToUpdate();

    assertThat(toUpdate)
      .hasSize(6)
      .extracting(TelemetryMetricsSentDto::getMetricKey, TelemetryMetricsSentDto::getDimension)
      .contains(tuple(key, dimension));
  }

  @DataProvider
  public static Object[][] shouldBeUpdatedMetrics() {
    return new Object[][]{
      {KEY_IS_VALID, DIMENSION_INSTALLATION, TimeUnit.DAYS, 100},
      {KEY_COVERAGE, DIMENSION_PROJECT, TimeUnit.DAYS, 100},
      {KEY_NCLOC, DIMENSION_LANGUAGE, TimeUnit.DAYS, 100}
    };
  }// 1 minute ago

  @DataProvider
  public static Object[][] shouldNotBeUpdatedMetrics() {
    return new Object[][]{
      {KEY_IS_VALID, DIMENSION_INSTALLATION, TimeUnit.HOURS, 1},
      {KEY_COVERAGE, DIMENSION_PROJECT, TimeUnit.DAYS, 5},
      {KEY_NCLOC, DIMENSION_LANGUAGE, TimeUnit.DAYS, 24}
    };
  }

  private static void assertProjectMetrics(BaseMessage message) {
    assertThat(message.getInstallationId()).isEqualTo(SERVER_ID);
    assertThat(message.getDimension()).isEqualTo(Dimension.PROJECT);
    assertThat((Set< ProjectMetric>) (Set<?>) message.getMetrics())
      .extracting(ProjectMetric::getKey, ProjectMetric::getGranularity, ProjectMetric::getType, ProjectMetric::getProjectUuid, ProjectMetric::getValue)
      .containsExactlyInAnyOrder(
        tuple(KEY_COVERAGE, Granularity.WEEKLY, TelemetryDataType.FLOAT, "module-1", 12.05f),
        tuple(KEY_COVERAGE, Granularity.WEEKLY, TelemetryDataType.FLOAT, "module-2", 12.05f)
      );
  }

  private static void assertLanguageMetrics(BaseMessage message) {
    assertThat(message.getInstallationId()).isEqualTo(SERVER_ID);
    assertThat(message.getDimension()).isEqualTo(Dimension.LANGUAGE);
    assertThat((Set< LanguageMetric>) (Set<?>) message.getMetrics())
      .extracting(LanguageMetric::getKey, LanguageMetric::getGranularity, LanguageMetric::getType, LanguageMetric::getLanguage, LanguageMetric::getValue)
      .containsExactlyInAnyOrder(
        tuple(KEY_NCLOC, Granularity.MONTHLY, TelemetryDataType.INTEGER, "java", 125),
        tuple(KEY_NCLOC, Granularity.MONTHLY, TelemetryDataType.INTEGER, "cpp", 125)
      );
  }

  private static void assertUserMetrics(BaseMessage message) {
    assertThat(message.getInstallationId()).isEqualTo(SERVER_ID);
    assertThat(message.getDimension()).isEqualTo(Dimension.USER);
    assertThat((Set<UserMetric>) (Set<?>) message.getMetrics())
      .extracting(UserMetric::getKey, UserMetric::getGranularity, UserMetric::getType, UserMetric::getUserUuid, UserMetric::getValue)
      .containsExactlyInAnyOrder(
        tuple(KEY_LAST_ACTIVE, Granularity.DAILY, TelemetryDataType.STRING, "user-1", "2024-01-01")
      );
  }

  private static void assertInstallationMetrics(BaseMessage message) {
    assertThat(message.getInstallationId()).isEqualTo(SERVER_ID);
    assertThat(message.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat((Set<InstallationMetric>) (Set<?>) message.getMetrics())
      .extracting(InstallationMetric::getKey, InstallationMetric::getGranularity, InstallationMetric::getType, InstallationMetric::getValue)
      .containsExactlyInAnyOrder(
        tuple(KEY_IS_VALID, Granularity.DAILY, TelemetryDataType.BOOLEAN, false),
        tuple(KEY_UPDATE_FINISHED, Granularity.ADHOC, TelemetryDataType.BOOLEAN, true)
      );
  }

  private <T> TelemetryDataProvider<T> getBean(String key, Dimension dimension, Granularity granularity, T value, String... keys) {
    return new TelemetryDataProvider<>() {
      @Override
      public String getMetricKey() {
        return key;
      }

      @Override
      public Dimension getDimension() {
        return dimension;
      }

      @Override
      public Granularity getGranularity() {
        return granularity;
      }

      @Override
      public TelemetryDataType getType() {
        if (value.getClass() == String.class) {
          return TelemetryDataType.STRING;
        } else if (value.getClass() == Integer.class) {
          return TelemetryDataType.INTEGER;
        } else if (value.getClass() == Float.class) {
          return TelemetryDataType.FLOAT;
        } else if (value.getClass() == Boolean.class) {
          return TelemetryDataType.BOOLEAN;
        } else {
          throw new IllegalArgumentException("Unsupported type: " + value.getClass());
        }
      }

      @Override
      public Optional<T> getValue() {
        if (granularity == Granularity.ADHOC && value.getClass() == Boolean.class && !((Boolean) value)) {
          return Optional.empty();
        }

        return Optional.of(value);
      }

      @Override
      public Map<String, T> getValues() {
        return Stream.of(keys)
          .collect(Collectors.toMap(
            key -> key,
            key -> value
          ));
      }
    };
  }

}
