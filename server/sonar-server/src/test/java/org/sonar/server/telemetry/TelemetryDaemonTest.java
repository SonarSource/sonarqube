/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.telemetry;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.organization.DefaultOrganizationProviderImpl;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.updatecenter.common.Version;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_FREQUENCY_IN_SECONDS;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;
import static org.sonar.test.JsonAssert.assertJson;

public class TelemetryDaemonTest {

  private static final long ONE_HOUR = 60 * 60 * 1_000L;
  private static final long ONE_DAY = 24 * ONE_HOUR;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public LogTester logger = new LogTester().setLevel(LoggerLevel.DEBUG);

  private TelemetryClient client = mock(TelemetryClient.class);
  private InternalProperties internalProperties = spy(new MapInternalProperties());
  private FakeServer server = new FakeServer();
  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private TestSystem2 system2 = new TestSystem2().setNow(System.currentTimeMillis());
  private MapSettings settings = new MapSettings();
  private ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(db.getDbClient(), es.client());
  private UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);

  private final TelemetryDataLoader communityDataLoader = new TelemetryDataLoader(server, db.getDbClient(), pluginRepository, new UserIndex(es.client(), system2),
    new ProjectMeasuresIndex(es.client(), null, system2), editionProvider, new DefaultOrganizationProviderImpl(db.getDbClient()), null);
  private TelemetryDaemon communityUnderTest = new TelemetryDaemon(communityDataLoader, client, settings.asConfig(), internalProperties, system2);

  private final LicenseReader licenseReader = mock(LicenseReader.class);
  private final TelemetryDataLoader commercialDataLoader = new TelemetryDataLoader(server, db.getDbClient(), pluginRepository, new UserIndex(es.client(), system2),
    new ProjectMeasuresIndex(es.client(), null, system2), editionProvider, new DefaultOrganizationProviderImpl(db.getDbClient()), licenseReader);
  private TelemetryDaemon commercialUnderTest = new TelemetryDaemon(commercialDataLoader, client, settings.asConfig(), internalProperties, system2);

  @After
  public void tearDown() {
    communityUnderTest.stop();
  }

  @Test
  public void send_telemetry_data() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    server.setId("AU-TpxcB-iU5OvuD2FL7");
    server.setVersion("7.5.4");
    List<PluginInfo> plugins = asList(newPlugin("java", "4.12.0.11033"), newPlugin("scmgit", "1.2"), new PluginInfo("other"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    IntStream.range(0, 3).forEach(i -> db.users().insertUser());
    db.users().insertUser(u -> u.setActive(false));
    userIndexer.indexOnStartup(emptySet());

    MetricDto lines = db.measures().insertMetric(m -> m.setKey(LINES_KEY));
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey(NCLOC_KEY));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    MetricDto nclocDistrib = db.measures().insertMetric(m -> m.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY));

    ComponentDto project1 = db.components().insertMainBranch(db.getDefaultOrganization());
    ComponentDto project1Branch = db.components().insertProjectBranch(project1);
    db.measures().insertLiveMeasure(project1, lines, m -> m.setValue(200d));
    db.measures().insertLiveMeasure(project1, ncloc, m -> m.setValue(100d));
    db.measures().insertLiveMeasure(project1, coverage, m -> m.setValue(80d));
    db.measures().insertLiveMeasure(project1, nclocDistrib, m -> m.setValue(null).setData("java=200;js=50"));

    ComponentDto project2 = db.components().insertMainBranch(db.getDefaultOrganization());
    db.measures().insertLiveMeasure(project2, lines, m -> m.setValue(300d));
    db.measures().insertLiveMeasure(project2, ncloc, m -> m.setValue(200d));
    db.measures().insertLiveMeasure(project2, coverage, m -> m.setValue(80d));
    db.measures().insertLiveMeasure(project2, nclocDistrib, m -> m.setValue(null).setData("java=300;kotlin=2500"));
    projectMeasuresIndexer.indexOnStartup(emptySet());

    communityUnderTest.start();

    ArgumentCaptor<String> jsonCaptor = captureJson();
    String json = jsonCaptor.getValue();
    assertJson(json).ignoreFields("database").isSimilarTo(getClass().getResource("telemetry-example.json"));
    assertJson(getClass().getResource("telemetry-example.json")).ignoreFields("database").isSimilarTo(json);
    assertDatabaseMetadata(json);
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Sharing of SonarQube statistics is enabled.");
  }

  private void assertDatabaseMetadata(String json) {
    try (DbSession dbSession = db.getDbClient().openSession(false)) {
      DatabaseMetaData metadata = dbSession.getConnection().getMetaData();
      assertJson(json).isSimilarTo("{\n" +
        "  \"database\": {\n" +
        "    \"name\": \"H2\",\n" +
        "    \"version\": \"" + metadata.getDatabaseProductVersion() + "\"\n" +
        "  }\n" +
        "}");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void take_biggest_long_living_branches() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    server.setId("AU-TpxcB-iU5OvuD2FL7").setVersion("7.5.4");
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey(NCLOC_KEY));
    ComponentDto project = db.components().insertMainBranch(db.getDefaultOrganization());
    ComponentDto longBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));
    ComponentDto shortBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(SHORT));
    db.measures().insertLiveMeasure(project, ncloc, m -> m.setValue(10d));
    db.measures().insertLiveMeasure(longBranch, ncloc, m -> m.setValue(20d));
    db.measures().insertLiveMeasure(shortBranch, ncloc, m -> m.setValue(30d));
    projectMeasuresIndexer.indexOnStartup(emptySet());

    communityUnderTest.start();

    ArgumentCaptor<String> jsonCaptor = captureJson();
    assertJson(jsonCaptor.getValue()).isSimilarTo("{\n" +
      "  \"ncloc\": 20\n" +
      "}\n");
  }

  @Test
  public void send_data_via_client_at_startup_after_initial_delay() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    communityUnderTest.start();

    verify(client, timeout(2_000).atLeastOnce()).upload(anyString());
  }

  @Test
  public void data_contains_no_license_type_on_community_edition() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");

    communityUnderTest.start();

    ArgumentCaptor<String> jsonCaptor = captureJson();
    assertThat(jsonCaptor.getValue()).doesNotContain("licenseType");
  }

  @Test
  public void data_contains_no_license_type_on_commercial_edition_if_no_license() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    when(licenseReader.read()).thenReturn(Optional.empty());

    commercialUnderTest.start();

    ArgumentCaptor<String> jsonCaptor = captureJson();
    assertThat(jsonCaptor.getValue()).doesNotContain("licenseType");
  }

  @Test
  public void data_has_license_type_on_commercial_edition_if_no_license() throws IOException {
    String licenseType = randomAlphabetic(12);
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    LicenseReader.License license = mock(LicenseReader.License.class);
    when(license.getType()).thenReturn(licenseType);
    when(licenseReader.read()).thenReturn(Optional.of(license));

    commercialUnderTest.start();

    ArgumentCaptor<String> jsonCaptor = captureJson();
    assertJson(jsonCaptor.getValue()).isSimilarTo("{\n" +
      "  \"licenseType\": \"" + licenseType + "\"\n" +
      "}\n");
  }

  @Test
  public void check_if_should_send_data_periodically() throws IOException {
    initTelemetrySettingsToDefaultValues();
    long now = system2.now();
    long sixDaysAgo = now - (ONE_DAY * 6L);
    long sevenDaysAgo = now - (ONE_DAY * 7L);
    internalProperties.write("telemetry.lastPing", String.valueOf(sixDaysAgo));
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    communityUnderTest.start();
    verify(client, after(2_000).never()).upload(anyString());
    internalProperties.write("telemetry.lastPing", String.valueOf(sevenDaysAgo));

    verify(client, timeout(2_000).atLeastOnce()).upload(anyString());
  }

  @Test
  public void send_server_id_and_version() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    String id = randomAlphanumeric(40);
    String version = randomAlphanumeric(10);
    server.setId(id);
    server.setVersion(version);
    communityUnderTest.start();

    ArgumentCaptor<String> json = captureJson();
    assertThat(json.getValue()).contains(id, version);
  }

  @Test
  public void do_not_send_data_if_last_ping_earlier_than_one_week_ago() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    long now = system2.now();
    long sixDaysAgo = now - (ONE_DAY * 6L);

    internalProperties.write("telemetry.lastPing", String.valueOf(sixDaysAgo));
    communityUnderTest.start();

    verify(client, after(2_000).never()).upload(anyString());
  }

  @Test
  public void send_data_if_last_ping_is_one_week_ago() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    long today = parseDate("2017-08-01").getTime();
    system2.setNow(today + 15 * ONE_HOUR);
    long sevenDaysAgo = today - (ONE_DAY * 7L);
    internalProperties.write("telemetry.lastPing", String.valueOf(sevenDaysAgo));
    reset(internalProperties);

    communityUnderTest.start();

    verify(internalProperties, timeout(4_000)).write("telemetry.lastPing", String.valueOf(today));
    verify(client).upload(anyString());
  }

  @Test
  public void opt_out_sent_once() throws IOException {
    initTelemetrySettingsToDefaultValues();
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    settings.setProperty("sonar.telemetry.enable", "false");
    communityUnderTest.start();
    communityUnderTest.start();

    verify(client, after(2_000).never()).upload(anyString());
    verify(client, timeout(2_000).times(1)).optOut(anyString());
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Sharing of SonarQube statistics is disabled.");
  }

  private PluginInfo newPlugin(String key, String version) {
    return new PluginInfo(key)
      .setVersion(Version.create(version));
  }

  private void initTelemetrySettingsToDefaultValues() {
    settings.setProperty(SONAR_TELEMETRY_ENABLE.getKey(), SONAR_TELEMETRY_ENABLE.getDefaultValue());
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), SONAR_TELEMETRY_URL.getDefaultValue());
    settings.setProperty(SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getKey(), SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getDefaultValue());
  }

  private ArgumentCaptor<String> captureJson() throws IOException {
    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(client, timeout(2_000).atLeastOnce()).upload(jsonCaptor.capture());
    return jsonCaptor;
  }
}
