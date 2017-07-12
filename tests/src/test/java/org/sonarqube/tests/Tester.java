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
package org.sonarqube.tests;

import com.sonar.orchestrator.Orchestrator;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.ws.client.WsClient;
import util.selenium.Selenese;

import static java.util.Objects.requireNonNull;
import static util.ItUtils.newUserWsClient;

/**
 * This JUnit rule wraps an {@link Orchestrator} instance and provides :
 * <ul>
 * <li>enabling the organization feature by default</li>
 * <li>clean-up of organizations between tests</li>
 * <li>clean-up of users between tests</li>
 * <li>clean-up of session when opening a browser (cookies, local storage)</li>
 * <li>quick access to {@link WsClient} instances</li>
 * <li>helpers to generate organizations and users</li>
 * </ul>
 * <p>
 * Recommendation is to define a {@code @Rule} instance. If not possible, then
 * {@code @ClassRule} must be used through a {@link org.junit.rules.RuleChain}
 * around {@link Orchestrator}.
 */
public class Tester extends ExternalResource implements Session {

  private final Orchestrator orchestrator;

  // configuration before startup
  private boolean disableOrganizations = false;
  private Elasticsearch elasticsearch = null;

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

  /**
   * Enables Elasticsearch debugging, see {@link #elasticsearch()}.
   *
   * The property "sonar.search.httpPort" must be defined before
   * starting SonarQube server.
   */
  public Tester setElasticsearchHttpPort(int port) {
    verifyNotStarted();
    elasticsearch = new Elasticsearch(port);
    return this;
  }

  @Override
  protected void before() {
    verifyNotStarted();
    rootSession = new SessionImpl(orchestrator, "admin", "admin");

    if (!disableOrganizations) {
      organizations().enableSupport();
    }

    beforeCalled = true;
  }

  @Override
  protected void after() {
    if (!disableOrganizations) {
      organizations().deleteNonGuardedOrganizations();
    }
    users().deleteAll();
    projects().deleteAll();
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

  public Elasticsearch elasticsearch() {
    return requireNonNull(elasticsearch, "Elasticsearch HTTP port is not defined. See #setElasticsearchHttpPort()");
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
  public ProjectTester projects() {
    return rootSession.projects();
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
    public ProjectTester projects() {
      return new ProjectTester(this);
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
