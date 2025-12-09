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
package org.sonarqube.ws.tester;

import com.google.common.base.Preconditions;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.ce.ActivityRequest;

import static com.sonar.orchestrator.container.Edition.DEVELOPER;
import static com.sonar.orchestrator.container.Edition.ENTERPRISE;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.sonarqube.ws.client.HttpConnector.DEFAULT_READ_TIMEOUT_MILLISECONDS;

/**
 * This JUnit rule wraps an {@link OrchestratorRule} instance and provides :
 * <ul>
 * <li>clean-up of users between tests</li>
 * <li>clean-up of session when opening a browser (cookies, local storage)</li>
 * <li>quick access to {@link WsClient} instances</li>
 * <li>clean-up of defined settings. Properties that are not defined by a plugin are not reset.</li>
 * <li>helpers to generate users</li>
 * </ul>
 * <p>
 * Recommendation is to define a {@code @Rule} instance. If not possible, then
 * {@code @ClassRule} must be used through a {@link org.junit.rules.RuleChain}
 * around {@link OrchestratorRule}.
 * <p>
 * Not supported:
 * <ul>
 * <li>clean-up global settings</li>
 * <li>clean-up system administrators/roots</li>
 * <li>clean-up the properties that are not defined (no PropertyDefinition)</li>
 * </ul>
 * When used with JUnit5, the tester can be started and stopped in the same pattern as Junit4 for @ClassRule or @Rule using the flag  #useJunit5ClassInitialization
 */
public class Tester extends ExternalResource implements TesterSession, BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {
  private static final String ADMIN_CRYPTED_PASSWORD = "100000$R9xDN18ebKxA3ZTaputi6wDt+fcKhP2h3GgAjGbcBlCSlkMLENxw9wziHS46QIW3fWOjEMpeyEts+pNuPXSbYA==";
  private static final String ADMIN_SALT = "pSDhsn3IM3KCa74CRRf7T7Vx+OE=";
  static final String FORCE_AUTHENTICATION_PROPERTY_NAME = "sonar.forceAuthentication";

  private final Orchestrator orchestrator;

  private boolean enableForceAuthentication = false;
  private Elasticsearch elasticsearch = null;

  // initialized in #before()
  private boolean beforeCalled = false;
  private TesterSession rootSession;

  private final int readTimeoutMilliseconds;

  /**
   * Defines if the tester is executed at class level or method level in the Junit5 test.
   * If true, the tester will be started and stopped at the class level.
   */
  private boolean classLevel = false;

  public Tester(OrchestratorRule orchestrator) {
    this(orchestrator, DEFAULT_READ_TIMEOUT_MILLISECONDS);
  }

  public Tester(OrchestratorRule orchestrator, int readTimeoutMilliseconds) {
    this(orchestrator.getOrchestrator(), readTimeoutMilliseconds);
  }

  public Tester(Orchestrator orchestrator) {
    this(orchestrator, DEFAULT_READ_TIMEOUT_MILLISECONDS);
  }

  public Tester(Orchestrator orchestrator, int readTimeoutMilliseconds) {
    this.orchestrator = orchestrator;
    this.readTimeoutMilliseconds = readTimeoutMilliseconds;
  }

  public Tester enableForceAuthentication() {
    verifyNotStarted();
    enableForceAuthentication = true;
    return this;
  }

  /**
   * Enable class level initialization for Junit5.
   * Should only be used with Junit5.
   *
   * @return Tester
   */
  public Tester withClassLevel() {
    classLevel = true;
    return this;
  }

  @Override
  public void before() {
    verifyNotStarted();
    rootSession = new TesterSessionImpl(orchestrator,
      httpConnectorBuilder -> httpConnectorBuilder.readTimeoutMilliseconds(readTimeoutMilliseconds),
      httpConnectorBuilder -> httpConnectorBuilder.credentials("admin", "admin"));

    setForceAuthentication(enableForceAuthentication);

    beforeCalled = true;
  }

  public void updateRootSession(String userName, String password) {
    rootSession = new TesterSessionImpl(orchestrator,
      httpConnectorBuilder -> httpConnectorBuilder.readTimeoutMilliseconds(readTimeoutMilliseconds),
      httpConnectorBuilder -> httpConnectorBuilder.credentials(userName, password));
  }

  @Override
  public void after() {
    waitForCeTasksToFinish();

    deactivateScim();

    users().deleteAll();
    projects().deleteAll();
    settings().deleteAll();
    qGates().deleteAll();
    qProfiles().deleteAll();
    webhooks().deleteAllGlobal();
    almSettings().deleteAll();
    groups().deleteAllGenerated();

    Edition edition = orchestrator.getDistribution().getEdition();
    if (edition.equals(ENTERPRISE)) {
      applications().deleteAll();
      views().deleteAll();
      applications().deleteAll();
    } else if (edition.equals(DEVELOPER)) {
      applications().deleteAll();
    }

    setForceAuthentication(enableForceAuthentication);
  }

  public void deactivateScim() {
    try (Connection connection = orchestrator.getDatabase().openConnection();
      PreparedStatement preparedStatement = connection.prepareStatement("delete from internal_properties where kee = ?")) {
      preparedStatement.setString(1, "sonar.scim.enabled");
      preparedStatement.execute();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public void resetRootPassword() {
    try (Connection connection = orchestrator.getDatabase().openConnection();
      PreparedStatement preparedStatement = connection.prepareStatement("update users set crypted_password=?, hash_method='PBKDF2', salt=?, reset_password=? where login =?")) {
      preparedStatement.setString(1, ADMIN_CRYPTED_PASSWORD);
      preparedStatement.setString(2, ADMIN_SALT);
      preparedStatement.setBoolean(3, true);
      preparedStatement.setString(4, "admin");
      preparedStatement.execute();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private void setForceAuthentication(boolean enableForceAuthentication) {
    String serverProperty = orchestrator.getDistribution().getServerProperty(FORCE_AUTHENTICATION_PROPERTY_NAME);
    if (serverProperty != null) {
      Preconditions.checkArgument(enableForceAuthentication == Boolean.parseBoolean(serverProperty),
        "This test was expecting to have authentication configured, but server property configuration has mismatched.");
      return;
    }

    if (enableForceAuthentication) {
      settings().resetSettings(FORCE_AUTHENTICATION_PROPERTY_NAME);
    } else {
      settings().setGlobalSetting(FORCE_AUTHENTICATION_PROPERTY_NAME, Boolean.toString(false));
    }
  }

  private void waitForCeTasksToFinish() {
    // Let's try to wait for 30s for in progress or pending tasks to finish
    int counter = 60;
    while (counter > 0 &&
           wsClient().ce().activity(new ActivityRequest().setStatus(List.of("PENDING", "IN_PROGRESS"))).getTasksCount() != 0) {
      counter--;
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    Ce.ActivityResponse activity = wsClient().ce().activity(new ActivityRequest().setStatus(List.of("PENDING", "IN_PROGRESS")));
    if (activity.getTasksCount() != 0) {
      throw new IllegalStateException(format("Waiting for 30 seconds for tasks to finish but there are still ce tasks : %n %s",
        activity.getTasksList().stream()
          .map(t -> format("analysisId: [%s] type: [%s] componentName: [%s]", t.getAnalysisId(), t.getType(), t.getComponentName()))
          .collect(Collectors.joining("\n"))));
    }
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

  public TesterSession withSystemPassCode(String systemPassCode) {
    verifyStarted();
    return new TesterSessionImpl(orchestrator, systemPassCode);
  }

  public Elasticsearch elasticsearch() {
    if (elasticsearch != null) {
      return elasticsearch;
    }
    elasticsearch = new Elasticsearch(orchestrator.getServer().getSearchPort());
    return elasticsearch;
  }

  private void verifyNotStarted() {
    if (beforeCalled) {
      throw new IllegalStateException("org.sonarqube.ws.tester.Tester should not be already started");
    }
  }

  private void verifyStarted() {
    if (!beforeCalled) {
      throw new IllegalStateException("org.sonarqube.ws.tester.Tester is not started yet");
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
  public NewCodePeriodTester newCodePeriods() {
    return rootSession.newCodePeriods();
  }

  @Override
  public QGateTester qGates() {
    return rootSession.qGates();
  }

  @Override
  public WebhookTester webhooks() {
    return rootSession.webhooks();
  }

  @Override
  public PermissionTester permissions() {
    return rootSession.permissions();
  }

  @Override
  public ViewTester views() {
    return rootSession.views();
  }

  @Override
  public ApplicationTester applications() {
    return rootSession.applications();
  }

  @Override
  public MeasureTester measures() {
    return rootSession.measures();
  }

  @Override
  public AlmSettingsTester almSettings() {
    return rootSession.almSettings();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (classLevel) {
      after();
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (!classLevel) {
      after();
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (classLevel) {
      before();
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (!classLevel) {
      before();
    }
  }

  private static class TesterSessionImpl implements TesterSession {
    private final WsClient client;

    private TesterSessionImpl(Orchestrator orchestrator, @Nullable String login, @Nullable String password) {
      Server server = orchestrator.getServer();
      this.client = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
        .acceptGzip(true)
        .url(server.getUrl())
        .credentials(login, password)
        .build());
    }

    private TesterSessionImpl(Orchestrator orchestrator, @Nullable String systemPassCode) {
      Server server = orchestrator.getServer();
      this.client = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
        .acceptGzip(true)
        .systemPassCode(systemPassCode)
        .url(server.getUrl())
        .build());
    }

    private TesterSessionImpl(Orchestrator orchestrator, Consumer<HttpConnector.Builder>... httpConnectorPopulators) {
      Server server = orchestrator.getServer();
      HttpConnector.Builder httpConnectorBuilder = HttpConnector.newBuilder()
        .acceptGzip(true)
        .url(server.getUrl());
      stream(httpConnectorPopulators).forEach(populator -> populator.accept(httpConnectorBuilder));
      this.client = WsClientFactories.getDefault().newClient(httpConnectorBuilder.build());
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
    public NewCodePeriodTester newCodePeriods() {
      return new NewCodePeriodTester(this);
    }

    @Override
    public QGateTester qGates() {
      return new QGateTester(this);
    }

    @Override
    public WebhookTester webhooks() {
      return new WebhookTester(this);
    }

    @Override
    public PermissionTester permissions() {
      return new PermissionTester(this);
    }

    @Override
    public ViewTester views() {
      return new ViewTester(this);
    }

    @Override
    public ApplicationTester applications() {
      return new ApplicationTester(this);
    }

    @Override
    public MeasureTester measures() {
      return new MeasureTester(this);
    }

    @Override
    public AlmSettingsTester almSettings() {
      return new AlmSettingsTester(this);
    }

  }
}
