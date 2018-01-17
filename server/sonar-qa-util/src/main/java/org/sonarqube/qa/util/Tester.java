/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.qa.util;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;

import static java.util.Objects.requireNonNull;

/**
 * This JUnit rule wraps an {@link Orchestrator} instance and provides :
 * <ul>
 * <li>enabling the organization feature by default</li>
 * <li>clean-up of organizations between tests</li>
 * <li>clean-up of users between tests</li>
 * <li>clean-up of session when opening a browser (cookies, local storage)</li>
 * <li>quick access to {@link WsClient} instances</li>
 * <li>clean-up of defined settings. Properties that are not defined by a plugin are not reset.</li>
 * <li>helpers to generate organizations and users</li>
 * </ul>
 *
 * Recommendation is to define a {@code @Rule} instance. If not possible, then
 * {@code @ClassRule} must be used through a {@link org.junit.rules.RuleChain}
 * around {@link Orchestrator}.
 *
 * Not supported:
 * <ul>
 *   <li>clean-up global settings</li>
 *   <li>clean-up system administrators/roots</li>
 *   <li>clean-up default organization</li>
 *   <li>clean-up the properties that are not defined (no PropertyDefinition)</li>
 * </ul>
 */
public class Tester extends ExternalResource implements TesterSession {

  private final Orchestrator orchestrator;

  // configuration before startup
  private boolean disableOrganizations = false;
  private Elasticsearch elasticsearch = null;

  // initialized in #before()
  private boolean beforeCalled = false;
  private TesterSession rootSession;

  public Tester(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
    String elasticsearchHttpPort = orchestrator.getDistribution().getServerProperty("sonar.search.httpPort");
    if (StringUtils.isNotBlank(elasticsearchHttpPort)) {
      this.elasticsearch = new Elasticsearch(Integer.parseInt(elasticsearchHttpPort));
    }
  }

  public Tester disableOrganizations() {
    verifyNotStarted();
    disableOrganizations = true;
    return this;
  }

  /**
   * Enables Elasticsearch debugging, see {@link #elasticsearch()}.
   * <p>
   * The property "sonar.search.httpPort" must be defined before
   * starting SonarQube server.
   */
  public Tester setElasticsearchHttpPort(int port) {
    verifyNotStarted();
    elasticsearch = new Elasticsearch(port);
    return this;
  }

  @Override
  public void before() {
    verifyNotStarted();
    rootSession = new TesterSessionImpl(orchestrator, "admin", "admin");

    if (!disableOrganizations) {
      organizations().enableSupport();
    }

    beforeCalled = true;
  }

  @Override
  public void after() {
    if (!disableOrganizations) {
      organizations().deleteNonGuardedOrganizations();
    }
    users().deleteAll();
    projects().deleteAll();
    settings().deleteAll();
    qGates().deleteAll();
  }

  public TesterSession asAnonymous() {
    return as(null, null);
  }

  public TesterSession as(String login) {
    return as(login, login);
  }

  public TesterSession as(String login, String password) {
    verifyStarted();
    return new TesterSessionImpl(orchestrator, login, password);
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

  public Navigation openBrowser(String path) {
    verifyStarted();
    return Navigation.create(orchestrator, path);
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
  public QModelTester qModel() {
    return rootSession.qModel();
  }

  @Override
  public QProfileTester qProfiles() {
    return rootSession.qProfiles();
  }

  @Override
  public UserTester users() {
    return rootSession.users();
  }

  @Override
  public SettingTester settings() {
    return rootSession.settings();
  }

  @Override
  public QGateTester qGates() {
    return rootSession.qGates();
  }

  private static class TesterSessionImpl implements TesterSession {
    private final WsClient client;

    private TesterSessionImpl(Orchestrator orchestrator, @Nullable String login, @Nullable String password) {
      Server server = orchestrator.getServer();
      this.client = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
        .url(server.getUrl())
        .credentials(login, password)
        .build());
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
    public QModelTester qModel() {
      return new QModelTester(this);
    }

    @Override
    public QProfileTester qProfiles() {
      return new QProfileTester(this);
    }

    @Override
    public UserTester users() {
      return new UserTester(this);
    }

    @Override
    public SettingTester settings() {
      return new SettingTester(this);
    }

    @Override
    public QGateTester qGates() {
      return new QGateTester(this);
    }
  }
}
