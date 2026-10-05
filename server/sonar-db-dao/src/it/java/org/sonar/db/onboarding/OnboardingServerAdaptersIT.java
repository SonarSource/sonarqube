/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.onboarding;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonarsource.onboarding.api.model.AnalysisMode;
import org.sonarsource.onboarding.api.model.OnboardingOverviewResponse;
import org.sonarsource.onboarding.api.model.OnboardingProjectRow;
import org.sonarsource.onboarding.api.model.OnboardingProjectsResponse;
import org.sonarsource.onboarding.api.model.OnboardingStatisticsResponse;
import org.sonarsource.onboarding.api.model.PlatformShare;
import org.sonarsource.onboarding.api.model.ScanStatus;
import org.sonarsource.onboarding.api.model.TimelinePoint;
import org.sonarsource.onboarding.server.ServerAlmDiscoveryProvider;
import org.sonarsource.onboarding.server.ServerOnboardingCountsProvider;
import org.sonarsource.onboarding.server.ServerOnboardingDataProvider;
import org.sonarsource.onboarding.server.ServerOnboardingTimelineProvider;
import org.sonarsource.onboarding.server.db.OnboardingDao;
import org.sonarsource.onboarding.server.db.OnboardingDbClient;
import org.sonarsource.onboarding.server.db.OnboardingMyBatisConfExtension;
import org.sonarsource.onboarding.server.db.OnboardingRows;
import org.sonarsource.onboarding.server.db.OnboardingSecretDecryptor;
import org.sonarsource.onboarding.shared.OnboardingScope;
import org.sonarsource.onboarding.shared.port.AlmRepoCountProvider;
import org.sonarsource.onboarding.shared.service.OnboardingOverviewService;
import org.sonarsource.onboarding.shared.service.OnboardingProjectsService;
import org.sonarsource.onboarding.shared.service.OnboardingStatisticsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Exercises the SQL behind the onboarding overview, statistics and projects endpoints against the SQS schema.
 *
 * <p>This class deliberately lives in the DAO module because the database CI matrix runs it against each
 * configured vendor, while the unification onboarding tests use a feature-local copy of the schema.
 */
class OnboardingServerAdaptersIT {

  private static final String GITHUB_URL = "https://github.example.com/api/v3";
  private static final long GITHUB_DISCOVERED_REPOS = 10L;

  private static final long ALPHA_CREATED_AT = millis("2025-01-15T10:00:00Z");
  private static final long ALPHA_FIRST_ANALYSIS_AT = millis("2025-02-10T08:00:00Z");
  private static final long ALPHA_LAST_ANALYSIS_DATE = millis("2025-04-02T07:00:00Z");
  private static final long BRAVO_CREATED_AT = millis("2025-02-20T10:00:00Z");
  private static final long BRAVO_ANALYSIS_DATE = millis("2025-03-05T07:00:00Z");
  private static final long CHARLIE_CREATED_AT = millis("2025-03-01T10:00:00Z");

  private static final OnboardingScope SCOPE = OnboardingScope.of(null);

  @RegisterExtension
  private final DbTester db = DbTester.createWithConfExtension(System2.INSTANCE, new OnboardingMyBatisConfExtension());

  private final List<OnboardingRows.AlmSetting> almSettingsSeenByDiscovery = new CopyOnWriteArrayList<>();
  private final OnboardingDbClient dbClient = new OnboardingDbClient(
    db.getDbClient().getMyBatis(),
    new DBSessionsImpl(db.getDbClient().getMyBatis()),
    List.of(new OnboardingDao()));

  private final ServerAlmDiscoveryProvider almDiscovery = new ServerAlmDiscoveryProvider(
    dbClient, value -> value, List.of(githubRepoCountProvider()));
  private final OnboardingOverviewService overviewService = new OnboardingOverviewService(
    new ServerOnboardingCountsProvider(dbClient), almDiscovery);
  private final OnboardingStatisticsService statisticsService = new OnboardingStatisticsService(
    new ServerOnboardingTimelineProvider(dbClient), almDiscovery, Clock.fixed(Instant.parse("2025-04-15T00:00:00Z"), ZoneOffset.UTC));
  private final OnboardingProjectsService projectsService = new OnboardingProjectsService(
    new ServerOnboardingDataProvider(dbClient));

  @Test
  void getOverview_whenInstanceIsEmpty_shouldReturnZeroCounts() {
    OnboardingOverviewResponse overview = overviewService.getOverview(SCOPE);

    assertThat(overview.getProgressPct()).isZero();
    assertThat(overview.getSteps().getDevopsPlatforms().getConfigured()).isZero();
    assertThat(overview.getSteps().getRepositories().getImported()).isZero();
    assertThat(overview.getSteps().getRepositories().getDiscovered()).isNull();
    assertThat(overview.getSteps().getProjects().getAnalyzed()).isZero();
    assertThat(overview.getSteps().getProjects().getNotScanned()).isZero();
    assertThat(overview.getSteps().getProjects().getNotImported()).isNull();
  }

  @Test
  void getStatistics_whenInstanceIsEmpty_shouldReturnEmptyTimeline() {
    OnboardingStatisticsResponse statistics = statisticsService.getStatistics(SCOPE, null, null);

    assertThat(statistics.getTimeline()).isEmpty();
    assertThat(statistics.getDiscoveredTotal()).isNull();
    assertThat(statistics.getDevopsPlatforms().getTotal()).isZero();
    assertThat(statistics.getDevopsPlatforms().getShares()).isEmpty();
  }

  @Test
  void getProjects_whenInstanceIsEmpty_shouldReturnNoRows() {
    OnboardingProjectsResponse projects = projectsService.getProjects(SCOPE, null, null, null, 1, 100);

    assertThat(projects.getPage().getTotal()).isZero();
    assertThat(projects.getProjects()).isEmpty();
  }

  @Test
  void getOverview_shouldCountProjectsBindingsAnalysesAndAlmSettings() {
    insertProjects();

    OnboardingOverviewResponse overview = overviewService.getOverview(SCOPE);

    assertThat(overview.getSteps().getDevopsPlatforms().getConfigured()).isEqualTo(2L);
    assertThat(overview.getSteps().getRepositories().getImported()).isEqualTo(2L);
    assertThat(overview.getSteps().getRepositories().getDiscovered()).isEqualTo(GITHUB_DISCOVERED_REPOS);
    assertThat(overview.getSteps().getRepositories().getPercent()).isEqualTo(20.0);
    assertThat(overview.getSteps().getProjects().getAnalyzed()).isEqualTo(2L);
    assertThat(overview.getSteps().getProjects().getNotScanned()).isEqualTo(1L);
    assertThat(overview.getSteps().getProjects().getNotImported()).isEqualTo(8L);
    assertThat(overview.getSteps().getProjects().getTotal()).isEqualTo(11L);
    assertThat(overview.getSteps().getProjects().getPercent()).isEqualTo(18.2);
    assertThat(overview.getProgressPct()).isEqualTo(19.1);
    assertThat(almSettingsSeenByDiscovery)
      .extracting(OnboardingRows.AlmSetting::alm, OnboardingRows.AlmSetting::url)
      .containsExactly(tuple("github", GITHUB_URL));
  }

  @Test
  void getStatistics_shouldBuildTimelineFromImportAndFirstAnalysisDates() {
    insertProjects();

    OnboardingStatisticsResponse statistics = statisticsService.getStatistics(SCOPE,
      OffsetDateTime.parse("2025-01-01T00:00:00Z"), OffsetDateTime.parse("2025-03-01T00:00:00Z"));

    assertThat(statistics.getDiscoveredTotal()).isEqualTo(GITHUB_DISCOVERED_REPOS);
    assertThat(statistics.getTimeline())
      .extracting(point -> point.getDate().toInstant(), TimelinePoint::getRepositoriesImported, TimelinePoint::getProjectsScanned)
      .containsExactly(
        tuple(Instant.parse("2025-01-01T00:00:00Z"), 1L, 0L),
        tuple(Instant.parse("2025-02-01T00:00:00Z"), 2L, 1L),
        tuple(Instant.parse("2025-03-01T00:00:00Z"), 3L, 2L));
    assertThat(statistics.getDevopsPlatforms().getTotal()).isEqualTo(3L);
    assertThat(statistics.getDevopsPlatforms().getShares())
      .extracting(PlatformShare::getPlatform, PlatformShare::getCount, PlatformShare::getPercentage)
      .containsExactly(
        tuple("azure_devops", 1L, 33.3),
        tuple("github", 1L, 33.3),
        tuple("NOT_BOUND", 1L, 33.3));
  }

  @Test
  void getProjects_shouldListEveryProjectWithItsScanStatusAndAnalysisMode() {
    insertProjects();

    OnboardingProjectsResponse projects = projectsService.getProjects(SCOPE, null, null, null, 1, 100);

    assertThat(projects.getPage().getTotal()).isEqualTo(3L);
    assertThat(projects.getProjects())
      .extracting(OnboardingProjectRow::getKey, OnboardingProjectRow::getName, OnboardingProjectRow::getAlm,
        OnboardingProjectRow::getScanStatus, OnboardingProjectRow::getAnalysisMode, OnboardingProjectRow::getLastScan)
      .containsExactly(
        tuple("alpha", "Alpha", "github", ScanStatus.SCANNED, AnalysisMode.CI, ALPHA_LAST_ANALYSIS_DATE),
        tuple("bravo", "Bravo", "azure_devops", ScanStatus.SCANNED, AnalysisMode.NONE, BRAVO_ANALYSIS_DATE),
        tuple("charlie", "Charlie", null, ScanStatus.NOT_SCANNED, AnalysisMode.NONE, null));
  }

  @Test
  void getProjects_whenFilteredOnCiAnalysis_shouldReturnOnlyProjectsWithDetectedCi() {
    insertProjects();

    OnboardingProjectsResponse projects = projectsService.getProjects(SCOPE, null, ScanStatus.SCANNED, AnalysisMode.CI, 1, 100);

    assertThat(projects.getProjects()).extracting(OnboardingProjectRow::getKey).containsExactly("alpha");
  }

  /**
   * Alpha is bound to GitHub and analysed by a detected CI, first in February and last in April. Bravo is bound to
   * Azure DevOps and analysed locally. Charlie is standalone and has only an unprocessed analysis.
   */
  private void insertProjects() {
    AlmSettingDto github = db.almSettings().insertGitHubAlmSetting(s -> s.setUrl(GITHUB_URL));
    AlmSettingDto azure = db.almSettings().insertAzureAlmSetting();

    ProjectData alpha = insertProject("alpha", "Alpha", ALPHA_CREATED_AT);
    db.almSettings().insertGitHubProjectAlmSetting(github, alpha.getProjectDto(), p -> p.setAlmRepo("acme/alpha"));
    db.components().insertSnapshot(alpha.getMainBranchDto(), s -> s.setLast(false)
      .setCreatedAt(ALPHA_FIRST_ANALYSIS_AT)
      .setAnalysisDate(ALPHA_FIRST_ANALYSIS_AT));
    SnapshotDto alphaLastAnalysis = db.components().insertSnapshot(alpha.getMainBranchDto(), s -> s.setLast(true)
      .setCreatedAt(ALPHA_LAST_ANALYSIS_DATE + 60_000L)
      .setAnalysisDate(ALPHA_LAST_ANALYSIS_DATE));
    insertDetectedCi(alphaLastAnalysis, "Github Actions");

    ProjectData bravo = insertProject("bravo", "Bravo", BRAVO_CREATED_AT);
    db.almSettings().insertAzureProjectAlmSetting(azure, bravo.getProjectDto(), p -> p.setAlmSlug("acme-project").setAlmRepo("bravo"));
    SnapshotDto bravoAnalysis = db.components().insertSnapshot(bravo.getMainBranchDto(), s -> s.setLast(true)
      .setCreatedAt(BRAVO_ANALYSIS_DATE + 60_000L)
      .setAnalysisDate(BRAVO_ANALYSIS_DATE));
    insertDetectedCi(bravoAnalysis, "undetected");

    ProjectData charlie = insertProject("charlie", "Charlie", CHARLIE_CREATED_AT);
    db.components().insertSnapshot(charlie.getMainBranchDto(), s -> s.setStatus(SnapshotDto.STATUS_UNPROCESSED)
      .setLast(false)
      .setCreatedAt(CHARLIE_CREATED_AT + 60_000L)
      .setAnalysisDate(CHARLIE_CREATED_AT + 60_000L));
  }

  private ProjectData insertProject(String key, String name, long createdAt) {
    return db.components().insertPrivateProject(c -> c.setKey(key).setName(name), p -> p.setCreatedAt(createdAt));
  }

  private void insertDetectedCi(SnapshotDto analysis, String detectedCi) {
    db.getDbClient().analysisPropertiesDao().insert(db.getSession(), new AnalysisPropertyDto()
      .setUuid(Uuids.createFast())
      .setAnalysisUuid(analysis.getUuid())
      .setKey("sonar.analysis.detectedci")
      .setValue(detectedCi));
    db.commit();
  }

  private AlmRepoCountProvider githubRepoCountProvider() {
    return new AlmRepoCountProvider() {
      @Override
      public String supportedAlm() {
        return "github";
      }

      @Override
      public OptionalLong fetchTotalCount(OnboardingRows.AlmSetting setting, OnboardingSecretDecryptor secretDecryptor) {
        almSettingsSeenByDiscovery.add(setting);
        return OptionalLong.of(GITHUB_DISCOVERED_REPOS);
      }
    };
  }

  private static long millis(String instant) {
    return Instant.parse(instant).toEpochMilli();
  }
}
