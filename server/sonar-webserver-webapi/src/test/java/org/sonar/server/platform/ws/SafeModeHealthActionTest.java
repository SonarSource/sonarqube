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
package org.sonar.server.platform.ws;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.System;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class SafeModeHealthActionTest {

  private final HealthChecker healthChecker = mock(HealthChecker.class);
  private final SystemPasscode systemPasscode = mock(SystemPasscode.class);
  private final WsActionTester underTest = new WsActionTester(new SafeModeHealthAction(new HealthActionSupport(healthChecker), systemPasscode));

  @Test
  void verify_definition() {
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
  void request_fails_with_ForbiddenException_when_PassCode_disabled_or_incorrect() {
    when(systemPasscode.isValid(any())).thenReturn(false);
    TestRequest request = underTest.newRequest();

    expectForbiddenException(() -> request.execute());
  }

  @ParameterizedTest
  @EnumSource(Health.Status.class)
  void request_succeeds_when_valid_passcode(Health.Status healthStatus) {
    authenticateWithPasscode();
    when(healthChecker.checkNode())
      .thenReturn(Health.builder()
        .setStatus(healthStatus)
        .build());
    TestRequest request = underTest.newRequest();

    request.execute();
  }

  @Test
  void verify_response_example() {
    authenticateWithPasscode();
    when(healthChecker.checkNode())
      .thenReturn(Health.builder()
        .setStatus(Health.Status.RED)
        .addCause("Application node app-1 is RED")
        .build());

    TestResponse response = underTest.newRequest().execute();

    assertJson(response.getInput())
      .ignoreFields("nodes")
      .isSimilarTo(underTest.getDef().responseExampleAsString());
  }

  @ParameterizedTest
  @EnumSource(Health.Status.class)
  void request_returns_status_and_causes_from_HealthChecker_checkNode_method(Health.Status healthStatus) {
    authenticateWithPasscode();
    Health.Builder builder = Health.builder()
      .setStatus(healthStatus);
    IntStream.range(0, new Random().nextInt(5)).mapToObj(i -> RandomStringUtils.secure().nextAlphanumeric(3)).forEach(builder::addCause);
    Health health = builder.build();
    when(healthChecker.checkNode()).thenReturn(health);
    TestRequest request = underTest.newRequest();

    System.HealthResponse healthResponse = request.executeProtobuf(System.HealthResponse.class);
    assertThat(healthResponse.getHealth().name()).isEqualTo(healthStatus.name());
    assertThat(health.getCauses()).isEqualTo(health.getCauses());
  }

  @ParameterizedTest
  @EnumSource(Health.Status.class)
  void response_contains_status_and_causes_from_HealthChecker_checkCluster(Health.Status healthStatus) {
    authenticateWithPasscode();
    String[] causes = IntStream.range(0, new Random().nextInt(33)).mapToObj(i -> secure().nextAlphanumeric(4)).toArray(String[]::new);
    Health.Builder healthBuilder = Health.builder()
      .setStatus(healthStatus);
    Arrays.stream(causes).forEach(healthBuilder::addCause);
    when(healthChecker.checkNode()).thenReturn(healthBuilder.build());

    System.HealthResponse clusterHealthResponse = underTest.newRequest().executeProtobuf(System.HealthResponse.class);
    assertThat(clusterHealthResponse.getHealth().name()).isEqualTo(healthStatus.name());
    assertThat(clusterHealthResponse.getCausesList())
      .extracting(System.Cause::getMessage)
      .containsOnly(causes);
  }

  private void expectForbiddenException(ThrowingCallable shouldRaiseThrowable) {
    assertThatThrownBy(shouldRaiseThrowable)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  private void authenticateWithPasscode() {
    when(systemPasscode.isValid(any())).thenReturn(true);
  }

}
