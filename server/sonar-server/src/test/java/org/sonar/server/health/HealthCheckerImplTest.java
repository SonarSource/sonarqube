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
package org.sonar.server.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.health.Health.Status.GREEN;
import static org.sonar.server.health.Health.Status.RED;
import static org.sonar.server.health.Health.Status.YELLOW;

public class HealthCheckerImplTest {

  private final Random random = new Random();

  @Test
  public void check_returns_green_status_without_any_cause_when_there_is_no_HealthCheck() {
    HealthCheckerImpl underTest = new HealthCheckerImpl();

    assertThat(underTest.check()).isEqualTo(Health.GREEN);
  }

  @Test
  public void checks_returns_GREEN_status_if_only_GREEN_statuses_returned_by_HealthChecks() {
    List<Health.Status> statuses = IntStream.range(1, 1 + random.nextInt(20)).mapToObj(i -> GREEN).collect(Collectors.toList());
    HealthCheckerImpl underTest = newHealthCheckerImpl(statuses.stream());

    assertThat(underTest.check().getStatus())
      .describedAs("%s should have been computed from %s statuses", GREEN, statuses)
      .isEqualTo(GREEN);
  }

  @Test
  public void checks_returns_YELLOW_status_if_only_GREEN_and_at_least_one_YELLOW_statuses_returned_by_HealthChecks() {
    List<Health.Status> statuses = new ArrayList<>();
    Stream.concat(
      IntStream.range(0, 1 + random.nextInt(20)).mapToObj(i -> YELLOW), // at least 1 YELLOW
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> GREEN)).forEach(statuses::add); // between 0 and 19 GREEN
    Collections.shuffle(statuses);
    HealthCheckerImpl underTest = newHealthCheckerImpl(statuses.stream());

    assertThat(underTest.check().getStatus())
      .describedAs("%s should have been computed from %s statuses", YELLOW, statuses)
      .isEqualTo(YELLOW);
  }

  @Test
  public void checks_returns_RED_status_if_at_least_one_RED_status_returned_by_HealthChecks() {
    List<Health.Status> statuses = new ArrayList<>();
    Stream.of(
      IntStream.range(0, 1 + random.nextInt(20)).mapToObj(i -> RED), // at least 1 RED
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> YELLOW), // between 0 and 19 YELLOW
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> GREEN) // between 0 and 19 GREEN
    ).flatMap(s -> s)
      .forEach(statuses::add);
    Collections.shuffle(statuses);
    HealthCheckerImpl underTest = newHealthCheckerImpl(statuses.stream());

    assertThat(underTest.check().getStatus())
      .describedAs("%s should have been computed from %s statuses", RED, statuses)
      .isEqualTo(RED);
  }

  @Test
  public void checks_returns_causes_of_all_HealthChecks_whichever_their_status() {
    HealthCheck[] healthChecks = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(s -> new StaticHealthCheck(IntStream.range(0, random.nextInt(3)).mapToObj(i -> RandomStringUtils.randomAlphanumeric(3)).toArray(String[]::new)))
      .map(HealthCheck.class::cast)
      .toArray(HealthCheck[]::new);
    String[] expected = Arrays.stream(healthChecks).map(HealthCheck::check).flatMap(s -> s.getCauses().stream()).toArray(String[]::new);

    HealthCheckerImpl underTest = new HealthCheckerImpl(healthChecks);

    assertThat(underTest.check().getCauses()).containsOnly(expected);
  }

  private HealthCheckerImpl newHealthCheckerImpl(Stream<Health.Status> statuses) {
    Stream<StaticHealthCheck> staticHealthCheckStream = statuses.map(StaticHealthCheck::new);
    return new HealthCheckerImpl(staticHealthCheckStream.map(HealthCheck.class::cast).toArray(HealthCheck[]::new));
  }

  private class StaticHealthCheck implements HealthCheck {
    private final Health health;

    public StaticHealthCheck(Health.Status status) {
      this.health = Health.newHealthCheckBuilder().setStatus(status).build();
    }

    public StaticHealthCheck(String... causes) {
      Health.Builder builder = Health.newHealthCheckBuilder().setStatus(Health.Status.values()[random.nextInt(3)]);
      Stream.of(causes).forEach(builder::addCause);
      this.health = builder.build();
    }

    @Override
    public Health check() {
      return health;
    }
  }
}
