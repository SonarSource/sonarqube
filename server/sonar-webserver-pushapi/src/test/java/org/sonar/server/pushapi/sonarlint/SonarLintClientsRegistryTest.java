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
package org.sonar.server.pushapi.sonarlint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.server.exceptions.ForbiddenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.test.EventAssert.assertThatEvent;

public class SonarLintClientsRegistryTest {
  private static final String EVENT_NAME = "RuleSetChanged";

  private final AsyncContext defaultAsyncContext = mock(AsyncContext.class);

  private final Set<String> exampleProjectUuids = Set.of("project1", "project2", "project3");
  private final Set<String> languageKeys = Set.of("language1", "language2", "language3");
  private final String USER_UUID = "userUuid";
  private final ServletResponse response = mock(ServletResponse.class);
  private final ServletOutputStream outputStream = mock(ServletOutputStream.class);

  private final SonarLintClientPermissionsValidator permissionsValidator = mock(SonarLintClientPermissionsValidator.class);
  private final SonarLintPushEventExecutorService sonarLintPushEventExecutorService = new SonarLintPushEventExecutorServiceImpl();

  private SonarLintClientsRegistry underTest;

  @Before
  public void before() {
    underTest = new SonarLintClientsRegistry(permissionsValidator);
  }

  @After
  public void after() {
    this.sonarLintPushEventExecutorService.shutdown();
  }

  @Test
  public void registerClientAndUnregister_changesNumberOfClients_andClosesClient() {
    SonarLintClient sonarLintClient = mock(SonarLintClient.class);

    underTest.registerClient(sonarLintClient);

    assertThat(underTest.countConnectedClients()).isEqualTo(1);
    assertThat(underTest.getClients()).contains(sonarLintClient);

    underTest.unregisterClient(sonarLintClient);

    assertThat(underTest.countConnectedClients()).isZero();
    assertThat(underTest.getClients()).isEmpty();
    verify(sonarLintClient).close();
  }

  @Test
  public void registering10Clients_10ClientsAreRegistered() {
    for (int i = 0; i < 10; i++) {
      AsyncContext newAsyncContext = mock(AsyncContext.class);
      SonarLintClient sonarLintClient = new SonarLintClient(sonarLintPushEventExecutorService, newAsyncContext, exampleProjectUuids, languageKeys, USER_UUID);
      underTest.registerClient(sonarLintClient);
    }

    assertThat(underTest.countConnectedClients()).isEqualTo(10);
  }

  @Test
  public void listen_givenOneClientInterestedInJavaEvents_sendAllJavaEvents() throws IOException {
    Set<String> javaLanguageKey = Set.of("java");
    when(defaultAsyncContext.getResponse()).thenReturn(response);
    when(response.getOutputStream()).thenReturn(outputStream);
    SonarLintClient sonarLintClient = new SonarLintClient(sonarLintPushEventExecutorService, defaultAsyncContext, exampleProjectUuids, javaLanguageKey, USER_UUID);

    underTest.registerClient(sonarLintClient);

    SonarLintPushEvent event1 = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "project1", "java");
    SonarLintPushEvent event2 = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "project2", "java");
    SonarLintPushEvent event3 = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "project3", "java");

    underTest.broadcastMessage(event1);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(outputStream).write(captor.capture());
    String message = new String(captor.getValue());
    assertThatEvent(message)
      .hasType(EVENT_NAME);

    clearInvocations(outputStream);

    underTest.broadcastMessage(event2);

    verify(outputStream).write(captor.capture());
    message = new String(captor.getValue());
    assertThatEvent(message)
      .hasType(EVENT_NAME);

    clearInvocations(outputStream);


    underTest.broadcastMessage(event3);

    verify(outputStream).write(captor.capture());
    message = new String(captor.getValue());
    assertThatEvent(message)
      .hasType(EVENT_NAME);
  }

  @Test
  public void listen_givenOneClientInterestedInJsEventsAndJavaEventGenerated_sendZeroEvents() throws IOException {
    Set<String> jsLanguageKey = Set.of("js");
    when(defaultAsyncContext.getResponse()).thenReturn(response);
    when(response.getOutputStream()).thenReturn(outputStream);
    SonarLintClient sonarLintClient = new SonarLintClient(sonarLintPushEventExecutorService, defaultAsyncContext, exampleProjectUuids, jsLanguageKey, USER_UUID);

    underTest.registerClient(sonarLintClient);

    SonarLintPushEvent event = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "project1", "java");

    underTest.broadcastMessage(event);

    verifyNoInteractions(outputStream);
  }

  @Test
  public void listen_givenOneClientInterestedInProjA_DontCheckPermissionsForProjB() throws IOException {
    when(defaultAsyncContext.getResponse()).thenReturn(response);
    when(response.getOutputStream()).thenReturn(outputStream);
    SonarLintClient sonarLintClient = new SonarLintClient(sonarLintPushEventExecutorService, defaultAsyncContext, Set.of("projA"), Set.of("java"), USER_UUID);

    underTest.registerClient(sonarLintClient);

    SonarLintPushEvent event1 = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "projA", "java");
    SonarLintPushEvent event2 = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "projB", "java");

    ArgumentCaptor<Set<String>> argument = ArgumentCaptor.forClass(Set.class);

    underTest.broadcastMessage(event1);
    underTest.broadcastMessage(event2);

    verify(permissionsValidator, times(1)).validateUserCanReceivePushEventForProjectUuids(anyString(), argument.capture());
    assertThat(argument.getValue()).hasSize(1).contains("projA");
  }

  @Test
  public void listen_givenUserNotPermittedToReceiveEvent_closeConnection() {
    SonarLintClient sonarLintClient = createSampleSLClient();
    underTest.registerClient(sonarLintClient);
    doThrow(new ForbiddenException("Access forbidden")).when(permissionsValidator).validateUserCanReceivePushEventForProjectUuids(anyString(), anySet());

    SonarLintPushEvent event = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "project1", "java");

    underTest.broadcastMessage(event);

    verify(sonarLintClient).close();
  }

  @Test
  public void listen_givenUnregisteredClient_closeConnection() throws IOException {
    SonarLintClient sonarLintClient = createSampleSLClient();
    underTest.registerClient(sonarLintClient);
    doThrow(new IOException("Broken pipe")).when(sonarLintClient).writeAndFlush(anyString());

    SonarLintPushEvent event = new SonarLintPushEvent(EVENT_NAME, "data".getBytes(StandardCharsets.UTF_8), "project1", "java");

    underTest.broadcastMessage(event);

    underTest.registerClient(sonarLintClient);
    doThrow(new IllegalStateException("Things went wrong")).when(sonarLintClient).writeAndFlush(anyString());

    underTest.broadcastMessage(event);

    verify(sonarLintClient, times(2)).close();
  }

  @Test
  public void broadcast_push_event_to_clients() throws IOException {
    SonarLintPushEvent event = new SonarLintPushEvent("event", "data".getBytes(StandardCharsets.UTF_8), "project2", null);

    SonarLintClient sonarLintClient = createSampleSLClient();
    underTest.registerClient(sonarLintClient);

    underTest.broadcastMessage(event);

    verify(permissionsValidator, times(1)).validateUserCanReceivePushEventForProjectUuids(anyString(), anySet());
    verify(sonarLintClient, times(1)).writeAndFlush(anyString());
  }

  @Test
  public void broadcast_skips_push_if_event_project_does_not_match_with_client() throws IOException {
    SonarLintPushEvent event = new SonarLintPushEvent("event", "data".getBytes(StandardCharsets.UTF_8), "project4", null);

    SonarLintClient sonarLintClient = createSampleSLClient();
    underTest.registerClient(sonarLintClient);

    underTest.broadcastMessage(event);

    verify(permissionsValidator, times(0)).validateUserCanReceivePushEventForProjectUuids(anyString(), anySet());
    verify(sonarLintClient, times(0)).close();
    verify(sonarLintClient, times(0)).writeAndFlush(anyString());
  }

  @Test
  public void broadcast_givenUserNotPermittedToReceiveSonarLintPushEvent_closeConnection() {
    SonarLintPushEvent event = new SonarLintPushEvent("event", "data".getBytes(StandardCharsets.UTF_8), "project1", null);

    SonarLintClient sonarLintClient = createSampleSLClient();
    underTest.registerClient(sonarLintClient);
    doThrow(new ForbiddenException("Access forbidden")).when(permissionsValidator).validateUserCanReceivePushEventForProjectUuids(anyString(), anySet());

    underTest.broadcastMessage(event);

    verify(sonarLintClient).close();
  }

  @Test
  public void broadcast_givenUnregisteredClient_closeConnection() throws IOException {
    SonarLintPushEvent event = new SonarLintPushEvent("event", "data".getBytes(StandardCharsets.UTF_8), "project1", null);

    SonarLintClient sonarLintClient = createSampleSLClient();
    underTest.registerClient(sonarLintClient);
    doThrow(new IOException("Broken pipe")).when(sonarLintClient).writeAndFlush(anyString());

    underTest.broadcastMessage(event);

    underTest.registerClient(sonarLintClient);
    doThrow(new IllegalStateException("Things went wrong")).when(sonarLintClient).writeAndFlush(anyString());

    underTest.broadcastMessage(event);

    verify(sonarLintClient, times(2)).close();
  }

  @Test
  public void registerClient_whenCalledFirstTime_addsAsyncListenerToClient() {
    SonarLintClient client = mock(SonarLintClient.class);
    underTest.registerClient(client);

    verify(client).addListener(any());
  }

  private SonarLintClient createSampleSLClient() {
    SonarLintClient mock = mock(SonarLintClient.class);
    when(mock.getLanguages()).thenReturn(Set.of("java"));
    when(mock.getClientProjectUuids()).thenReturn(exampleProjectUuids);
    when(mock.getUserUuid()).thenReturn("userUuid");
    return mock;
  }
}
