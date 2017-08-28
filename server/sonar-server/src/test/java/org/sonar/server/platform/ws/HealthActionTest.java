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
package org.sonar.server.platform.ws;

import java.util.Random;
import java.util.stream.IntStream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.WsSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.health.Health.newHealthCheckBuilder;

public class HealthActionTest {
  private HealthChecker mockedHealthChecker = mock(HealthChecker.class);
  private WsActionTester underTest = new WsActionTester(new HealthAction(mockedHealthChecker));

  @Test
  public void verify_definition() {
    WebService.Action definition = underTest.getDef();

    assertThat(definition.key()).isEqualTo("health");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.description()).isNotEmpty();
    assertThat(definition.since()).isEqualTo("6.6");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExample()).isNotNull();
    assertThat(definition.params()).isEmpty();
  }

  @Test
  public void verify_example() {
    when(mockedHealthChecker.checkNode()).thenReturn(
        newHealthCheckBuilder()
            .setStatus(Health.Status.YELLOW)
            .addCause("Elasticsearch status is YELLOW")
            .build());
    TestRequest request = underTest.newRequest();

    JsonAssert.assertJson(request.execute().getInput())
        .isSimilarTo(underTest.getDef().responseExampleAsString());
  }

  @Test
  public void request_returns_status_and_causes_from_HealthChecker_checkNode_method() {
    Health.Status randomStatus = Health.Status.values()[new Random().nextInt(Health.Status.values().length)];
    Health.Builder builder = newHealthCheckBuilder()
        .setStatus(randomStatus);
    IntStream.range(0, new Random().nextInt(5)).mapToObj(i -> RandomStringUtils.randomAlphanumeric(3)).forEach(builder::addCause);
    Health health = builder.build();
    when(mockedHealthChecker.checkNode()).thenReturn(health);
    TestRequest request = underTest.newRequest();

    WsSystem.HealthResponse healthResponse = request.executeProtobuf(WsSystem.HealthResponse.class);
    assertThat(healthResponse.getHealth().name()).isEqualTo(randomStatus.name());
    assertThat(health.getCauses()).isEqualTo(health.getCauses());
  }

}
