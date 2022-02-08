/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.Set;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.core.util.RuleChange;
import org.sonar.core.util.RuleSetChangeEvent;
import org.sonar.server.pushapi.qualityprofile.StandaloneRuleActivatorEventsDistributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SonarLintClientsRegistryTest {

  private final AsyncContext defaultAsyncContext = mock(AsyncContext.class);

  private final Set<String> exampleKeys = Set.of("project1", "project2", "project3");

  private final Set<String> languageKeys = Set.of("language1", "language2", "language3");

  private SonarLintClientsRegistry underTest;

  @Before
  public void before() {
    underTest = new SonarLintClientsRegistry(mock(StandaloneRuleActivatorEventsDistributor.class));
  }

  @Test
  public void registerClientAndUnregister_changesNumberOfClients() {
    SonarLintClient sonarLintClient = new SonarLintClient(defaultAsyncContext, exampleKeys, languageKeys);

    underTest.registerClient(sonarLintClient);

    assertThat(underTest.countConnectedClients()).isEqualTo(1);

    underTest.unregisterClient(sonarLintClient);

    assertThat(underTest.countConnectedClients()).isZero();
  }

  @Test
  public void registering10Clients_10ClientsAreRegistered() {
    for (int i = 0; i < 10; i++) {
      AsyncContext newAsyncContext = mock(AsyncContext.class);
      SonarLintClient sonarLintClient = new SonarLintClient(newAsyncContext, exampleKeys, languageKeys);
      underTest.registerClient(sonarLintClient);
    }

    assertThat(underTest.countConnectedClients()).isEqualTo(10);
  }

  @Test
  public void listen_givenOneClientInterestedInJavaEvents_sendOneJavaEvent() throws IOException {
    Set<String> javaLanguageKey = Set.of("java");
    ServletResponse response = mock(ServletResponse.class);
    when(defaultAsyncContext.getResponse()).thenReturn(response);
    ServletOutputStream outputStream = mock(ServletOutputStream.class);
    when(response.getOutputStream()).thenReturn(outputStream);
    SonarLintClient sonarLintClient = new SonarLintClient(defaultAsyncContext, exampleKeys, javaLanguageKey);

    underTest.registerClient(sonarLintClient);

    RuleChange javaRule = createRuleChange("java");

    RuleChange[] activatedRules = {};
    RuleChange[] deactivatedRules = {javaRule};
    RuleSetChangeEvent ruleChangeEvent = new RuleSetChangeEvent(exampleKeys.toArray(String[]::new), activatedRules, deactivatedRules);
    underTest.listen(ruleChangeEvent);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(outputStream).write(captor.capture());
    String message = new String(captor.getValue());
    assertThat(message).contains("java");
  }

  @Test
  public void listen_givenOneClientInterestedInJsEventsAndJavaEventGenerated_sendZeroEvents() throws IOException {
    Set<String> jsLanguageKey = Set.of("js");
    ServletResponse response = mock(ServletResponse.class);
    when(defaultAsyncContext.getResponse()).thenReturn(response);
    ServletOutputStream outputStream = mock(ServletOutputStream.class);
    when(response.getOutputStream()).thenReturn(outputStream);
    SonarLintClient sonarLintClient = new SonarLintClient(defaultAsyncContext, exampleKeys, jsLanguageKey);

    underTest.registerClient(sonarLintClient);

    RuleChange javaRuleChange = createRuleChange("java");

    RuleChange[] activatedRules = {};
    RuleChange[] deactivatedRules = {javaRuleChange};
    RuleSetChangeEvent ruleChangeEvent = new RuleSetChangeEvent(exampleKeys.toArray(String[]::new), activatedRules, deactivatedRules);
    underTest.listen(ruleChangeEvent);

    verifyNoInteractions(outputStream);
  }

  private RuleChange createRuleChange(String language) {
    RuleChange javaRule = new RuleChange();
    javaRule.setLanguage(language);

    return javaRule;
  }

}
