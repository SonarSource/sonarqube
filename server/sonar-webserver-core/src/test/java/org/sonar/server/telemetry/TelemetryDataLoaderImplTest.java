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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
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
import org.sonar.server.platform.DockerSupport;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.updatecenter.common.Version;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.core.platform.EditionProvider.Edition.DEVELOPER;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

public class TelemetryDataLoaderImplTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private final FakeServer server = new FakeServer();
  private final PluginRepository pluginRepository = mock(PluginRepository.class);
  private final TestSystem2 system2 = new TestSystem2().setNow(System.currentTimeMillis());
  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final DockerSupport dockerSupport = mock(DockerSupport.class);
  private final InternalProperties internalProperties = spy(new MapInternalProperties());
  private final ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(db.getDbClient(), es.client());
  private final UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private final LicenseReader licenseReader = mock(LicenseReader.class);

  private final TelemetryDataLoader communityUnderTest = new TelemetryDataLoaderImpl(server, db.getDbClient(), pluginRepository, new UserIndex(es.client(), system2),
    new ProjectMeasuresIndex(es.client(), null, system2), editionProvider, new DefaultOrganizationProviderImpl(db.getDbClient()), internalProperties, dockerSupport, null);
  private final TelemetryDataLoader commercialUnderTest = new TelemetryDataLoaderImpl(server, db.getDbClient(), pluginRepository, new UserIndex(es.client(), system2),
    new ProjectMeasuresIndex(es.client(), null, system2), editionProvider, new DefaultOrganizationProviderImpl(db.getDbClient()), internalProperties, dockerSupport, licenseReader);

  @Test
  public void send_telemetry_data() {
    String serverId = "AU-TpxcB-iU5OvuD2FL7";
    String version = "7.5.4";
    server.setId(serverId);
    server.setVersion(version);
    List<PluginInfo> plugins = asList(newPlugin("java", "4.12.0.11033"), newPlugin("scmgit", "1.2"), new PluginInfo("other"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);
    when(editionProvider.get()).thenReturn(Optional.of(DEVELOPER));

    int userCount = 3;
    IntStream.range(0, userCount).forEach(i -> db.users().insertUser());
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

    TelemetryData data = communityUnderTest.load();
    assertThat(data.getServerId()).isEqualTo(serverId);
    assertThat(data.getVersion()).isEqualTo(version);
    assertThat(data.getEdition()).contains(DEVELOPER);
    assertDatabaseMetadata(data.getDatabase());
    assertThat(data.getPlugins()).containsOnly(
      entry("java", "4.12.0.11033"), entry("scmgit", "1.2"), entry("other", "undefined"));
    assertThat(data.getUserCount()).isEqualTo(userCount);
    assertThat(data.getProjectCount()).isEqualTo(2L);
    assertThat(data.getNcloc()).isEqualTo(300L);
    assertThat(data.getProjectCountByLanguage()).containsOnly(
      entry("java", 2L), entry("kotlin", 1L), entry("js", 1L));
    assertThat(data.getNclocByLanguage()).containsOnly(
      entry("java", 500L), entry("kotlin", 2500L), entry("js", 50L));
    assertThat(data.isInDocker()).isFalse();
  }

  private void assertDatabaseMetadata(TelemetryData.Database database) {
    try (DbSession dbSession = db.getDbClient().openSession(false)) {
      DatabaseMetaData metadata = dbSession.getConnection().getMetaData();
      assertThat(database.getName()).isEqualTo("H2");
      assertThat(database.getVersion()).isEqualTo(metadata.getDatabaseProductVersion());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void take_largest_branches() {
    server.setId("AU-TpxcB-iU5OvuD2FL7").setVersion("7.5.4");
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey(NCLOC_KEY));
    ComponentDto project = db.components().insertMainBranch(db.getDefaultOrganization());
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST));
    db.measures().insertLiveMeasure(project, ncloc, m -> m.setValue(10d));
    db.measures().insertLiveMeasure(branch1, ncloc, m -> m.setValue(20d));
    db.measures().insertLiveMeasure(pr, ncloc, m -> m.setValue(30d));
    projectMeasuresIndexer.indexOnStartup(emptySet());

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getNcloc()).isEqualTo(20L);
  }

  @Test
  public void data_contains_no_license_type_on_community_edition() {
    TelemetryData data = communityUnderTest.load();

    assertThat(data.getLicenseType()).isEmpty();
  }

  @Test
  public void data_contains_no_license_type_on_commercial_edition_if_no_license() {
    when(licenseReader.read()).thenReturn(Optional.empty());

    TelemetryData data = commercialUnderTest.load();

    assertThat(data.getLicenseType()).isEmpty();
  }

  @Test
  public void data_has_license_type_on_commercial_edition_if_no_license() {
    String licenseType = randomAlphabetic(12);
    LicenseReader.License license = mock(LicenseReader.License.class);
    when(license.getType()).thenReturn(licenseType);
    when(licenseReader.read()).thenReturn(Optional.of(license));

    TelemetryData data = commercialUnderTest.load();

    assertThat(data.getLicenseType()).contains(licenseType);
  }

  @Test
  public void send_server_id_and_version() {
    String id = randomAlphanumeric(40);
    String version = randomAlphanumeric(10);
    server.setId(id);
    server.setVersion(version);

    TelemetryData data = communityUnderTest.load();
    assertThat(data.getServerId()).isEqualTo(id);
    assertThat(data.getVersion()).isEqualTo(version);

    data = commercialUnderTest.load();
    assertThat(data.getServerId()).isEqualTo(id);
    assertThat(data.getVersion()).isEqualTo(version);
  }

  @Test
  public void send_server_installation_date_and_installation_version() {
    String installationVersion = "7.9.BEST.LTS.EVER";
    Long installationDate = 1546300800000L; // 2019/01/01
    internalProperties.write(InternalProperties.INSTALLATION_DATE, String.valueOf(installationDate));
    internalProperties.write(InternalProperties.INSTALLATION_VERSION, installationVersion);

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getInstallationDate()).isEqualTo(installationDate);
    assertThat(data.getInstallationVersion()).isEqualTo(installationVersion);
  }

  @Test
  public void do_not_send_server_installation_details_if_missing_property() {
    TelemetryData data = communityUnderTest.load();
    assertThat(data.getInstallationDate()).isNull();
    assertThat(data.getInstallationVersion()).isNull();

    data = commercialUnderTest.load();
    assertThat(data.getInstallationDate()).isNull();
    assertThat(data.getInstallationVersion()).isNull();
  }

  private PluginInfo newPlugin(String key, String version) {
    return new PluginInfo(key)
      .setVersion(Version.create(version));
  }

}
