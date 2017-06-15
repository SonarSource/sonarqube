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
package org.sonarqube.test;

import com.sonar.orchestrator.Orchestrator;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.setting.SetRequest;
import pageobjects.Navigation;
import util.selenium.Selenese;

import static util.ItUtils.newUserWsClient;

public class Tester extends ExternalResource implements Session {

  private final Orchestrator orchestrator;

  // configuration before startup
  private boolean disableOrganizations = false;
  private boolean enableOnBoardingTutorials = false;

  // initialized in #before()
  private boolean beforeCalled = false;
  private Session rootSession;

  public Tester(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public Tester disableOrganizations() {
    verifyNotStarted();
    disableOrganizations = true;
    return this;
  }

  public Tester enableOnBoardingTutorials() {
    verifyNotStarted();
    enableOnBoardingTutorials = true;
    return this;
  }

  @Override
  protected void before() {
    verifyNotStarted();
    rootSession = new SessionImpl(orchestrator, "admin", "admin");

    if (!disableOrganizations) {
      organizations().enableSupport();
    }

    if (!enableOnBoardingTutorials) {
      rootSession.wsClient().settings().set(SetRequest.builder()
        .setKey("sonar.onboardingTutorial.showToNewUsers")
        .setValue("false")
        .build());
      rootSession.wsClient().users().skipOnboardingTutorial();
    }

    beforeCalled = true;
  }

  @Override
  protected void after() {
    if (!disableOrganizations) {
      organizations().deleteNonGuardedOrganizations();
    }
    users().deleteAll();
  }

  public Session asAnonymous() {
    return as(null, null);
  }

  public Session as(String login) {
    return as(login, login);
  }

  public Session as(String login, String password) {
    verifyStarted();
    return new SessionImpl(orchestrator, login, password);
  }

  /**
   * Open a new browser session. Cookies are deleted.
   */
  public Navigation openBrowser() {
    verifyStarted();
    return Navigation.create(orchestrator);
  }

  /**
   * @deprecated use Selenide tests with {@link #openBrowser()}
   */
  @Deprecated
  public Tester runHtmlTests(String... htmlTests) {
    Selenese.runSelenese(orchestrator, htmlTests);
    return this;
  }

  private void verifyNotStarted() {
    if (beforeCalled) {
      throw new IllegalStateException("Orchestrator should not be already started");
    }
  }

  private void verifyStarted() {
    if (!beforeCalled) {
      throw new IllegalStateException("Orchestrator is not started yet");
    }
  }

  /**
   * Web service client configured with root access
   */
  @Override
  public WsClient wsClient() {
    verifyStarted();
    return rootSession.wsClient();
  }

  @Override
  public GroupTester groups() {
    return rootSession.groups();
  }

  @Override
  public OrganizationTester organizations() {
    return rootSession.organizations();
  }

  @Override
  public QProfileTester qProfiles() {
    return rootSession.qProfiles();
  }

  @Override
  public UserTester users() {
    return rootSession.users();
  }

  private static class SessionImpl implements Session {
    private final WsClient client;

    private SessionImpl(Orchestrator orchestrator, @Nullable String login, @Nullable String password) {
      this.client = newUserWsClient(orchestrator, login, password);
    }

    @Override
    public WsClient wsClient() {
      return client;
    }

    @Override
    public GroupTester groups() {
      return new GroupTester(this);
    }

    @Override
    public OrganizationTester organizations() {
      return new OrganizationTester(this);
    }

    @Override
    public QProfileTester qProfiles() {
      return new QProfileTester(this);
    }

    @Override
    public UserTester users() {
      return new UserTester(this);
    }
  }
}
