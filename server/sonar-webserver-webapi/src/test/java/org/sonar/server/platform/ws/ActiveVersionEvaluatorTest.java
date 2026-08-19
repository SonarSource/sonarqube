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
package org.sonar.server.platform.ws;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.Version.parse;

class ActiveVersionEvaluatorTest {

  private final SonarQubeVersion sonarQubeVersion = mock(SonarQubeVersion.class);
  private final UpdateCenter updateCenter = mock(UpdateCenter.class);
  private static final Sonar sonar = mock(Sonar.class);
  private final System2 system2 = mock(System2.class);
  private final ActiveVersionEvaluator underTest = new ActiveVersionEvaluator(sonarQubeVersion, system2);

  @BeforeEach
  void setup() {
    when(updateCenter.getSonar()).thenReturn(sonar);
    when(updateCenter.getDate()).thenReturn(DateUtils.parseDateTime("2015-04-24T16:08:36+0200"));
    when(sonar.getLtaVersion()).thenReturn(new Release(sonar, Version.create("9.9.4")));
    when(sonar.getPastLtaVersion()).thenReturn(new Release(sonar, Version.create("8.9.10")));
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsLatestLta_shouldReturnActiveVersion() {
    when(updateCenter.getSonar().getAllReleases(any())).thenReturn(getReleases());
    when(sonarQubeVersion.get()).thenReturn(parse("9.9.2"));

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsLtaLineWithFutureEolDate_shouldReturnActive() {
    when(sonarQubeVersion.get()).thenReturn(parse("2026.1"));
    when(system2.now()).thenReturn(LocalDate.of(2027, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    Release installedLtaRelease = ltaReleaseWithEolDate("2026.1", LocalDate.of(2027, Month.JULY, 27));
    Release newerLtaRelease = ltaReleaseWithEolDate("2026.5", LocalDate.of(2028, Month.JANUARY, 1));
    when(sonar.getLtaVersions()).thenReturn(List.of(installedLtaRelease, newerLtaRelease));

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsLtaLineWithEolDateExactlyToday_shouldReturnActive() {
    when(sonarQubeVersion.get()).thenReturn(parse("2026.1"));
    // noon, not midnight: the whole eolDate day must stay active regardless of the current time-of-day
    Instant laterOnEolDate = LocalDate.of(2027, Month.JULY, 27).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant();
    when(system2.now()).thenReturn(laterOnEolDate.toEpochMilli());
    when(sonar.getLtaVersions()).thenReturn(List.of(ltaReleaseWithEolDate("2026.1", LocalDate.of(2027, Month.JULY, 27))));

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsLtaLineWithPastEolDate_shouldReturnNotActive() {
    when(sonarQubeVersion.get()).thenReturn(parse("2026.1"));
    when(system2.now()).thenReturn(LocalDate.of(2027, Month.AUGUST, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    when(sonar.getLtaVersions()).thenReturn(List.of(ltaReleaseWithEolDate("2026.1", LocalDate.of(2027, Month.JULY, 27))));

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isFalse();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsSecondOldestOfThreeLtaLinesButNotYetEol_shouldReturnActive() {
    // reproduces the SONAR-30922 bug scenario: with 3 concurrently supported LTAs, the middle one must stay
    // active based on its own eolDate, independently of the newest LTA's release date.
    when(sonarQubeVersion.get()).thenReturn(parse("2026.1"));
    when(system2.now()).thenReturn(LocalDate.of(2027, Month.MARCH, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    Release oldestLtaRelease = ltaReleaseWithEolDate("2025.4", LocalDate.of(2026, Month.JULY, 1));
    Release installedLtaRelease = ltaReleaseWithEolDate("2026.1", LocalDate.of(2027, Month.JULY, 27));
    Release newestLtaRelease = ltaReleaseWithEolDate("2026.5", LocalDate.of(2028, Month.JANUARY, 1));
    when(sonar.getLtaVersions()).thenReturn(List.of(oldestLtaRelease, installedLtaRelease, newestLtaRelease));

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionMatchesLtaLineWithoutEolDateYet_shouldFallBackToLegacySixMonthLogic() {
    Date fiveMonthsAgo = Date.from(LocalDate.of(2024, Month.JANUARY, 1).minusMonths(5).atStartOfDay(ZoneOffset.UTC).toInstant());
    when(system2.now()).thenReturn(LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
    when(sonarQubeVersion.get()).thenReturn(parse("8.9.5"));
    // an ltaVersions entry exists for the installed line, but has no eolDate yet (e.g. an update-center.properties
    // file mid-rollout) - the legacy relative algorithm must still apply, exactly as if the entry weren't there.
    when(sonar.getLtaVersions()).thenReturn(List.of(new Release(sonar, Version.create("8.9"))));
    SortedSet<Release> releases = getReleases();
    releases.stream().filter(r -> r.getVersion().equals(Version.create("9.9"))).findFirst().get().setDate(fiveMonthsAgo);
    when(sonar.getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  private static Release ltaReleaseWithEolDate(String version, LocalDate eolDate) {
    return new Release(sonar, Version.create(version)).setEolDate(Date.from(eolDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsPastLtaAndWithinSixMonthFromLta_shouldReturnVersionIsActive() {
    Date fiveMonthsAgo = Date.from(LocalDate.of(2024, Month.JANUARY, 1).minusMonths(5).atStartOfDay(ZoneOffset.UTC).toInstant());

    when(system2.now()).thenReturn(LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());

    when(sonarQubeVersion.get()).thenReturn(parse("8.9.5"));
    SortedSet<Release> releases = getReleases();
    releases.stream().filter(r -> r.getVersion().equals(Version.create("9.9"))).findFirst().get().setDate(fiveMonthsAgo);
    when(sonar.getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsPastLtaAndAfterSixMonthFromLta_shouldReturnVersionNotActive() {
    Date sevenMonthsAgo = Date.from(LocalDate.of(2024, Month.JANUARY, 1).minusMonths(7).atStartOfDay(ZoneOffset.UTC).toInstant());

    when(system2.now()).thenReturn(LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());

    when(sonarQubeVersion.get()).thenReturn(parse("8.9.5"));
    SortedSet<Release> releases = getReleases();
    releases.stream().filter(r -> r.getVersion().equals(Version.create("9.9"))).findFirst().get().setDate(sevenMonthsAgo);
    when(sonar.getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isFalse();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsPastLtaAndReleaseDateIsMissing_shouldThrowIllegalStateException() {

    when(sonarQubeVersion.get()).thenReturn(parse("8.9.5"));
    SortedSet<Release> releases = getReleases();
    when(sonar.getAllReleases(any())).thenReturn(releases);

    assertThatThrownBy(() -> underTest.evaluateIfActiveVersion(updateCenter))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Initial Major release date is missing in releases");
  }

  @Test
  void evaluateIfActiveVersion_whenNoReleasesFound_shouldThrowIllegalStateException() {

    when(sonarQubeVersion.get()).thenReturn(parse("10.8.0"));

    when(sonar.getAllReleases(any())).thenReturn(Collections.emptySortedSet());

    assertThatThrownBy(() -> underTest.evaluateIfActiveVersion(updateCenter))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Unable to find previous release in releases");
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsLatestMinusOne_shouldReturnVersionIsActive() {
    when(sonarQubeVersion.get()).thenReturn(parse("10.9"));
    when(updateCenter.getSonar().getAllReleases(any())).thenReturn(getReleases());

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsSnapshot_shouldReturnVersionIsActive() {
    when(sonarQubeVersion.get()).thenReturn(parse("10.11-SNAPSHOT"));
    when(updateCenter.getSonar().getAllReleases(any())).thenReturn(getReleases());

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsTheOnlyAvailableVersion_shouldReturnVersionIsActive() {
    TreeSet<Release> releases = new TreeSet<>();
    releases.add(new Release(sonar, Version.create("10.8.0.12345")));

    when(sonarQubeVersion.get()).thenReturn(parse("10.8.0.12345"));
    when(updateCenter.getSonar().getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenAvailableVersionsAreAllPatchesOfInstalledVersion_shouldReturnVersionIsActive() {
    TreeSet<Release> releases = new TreeSet<>();
    releases.add(new Release(sonar, Version.create("10.8.0.12345")));
    releases.add(new Release(sonar, Version.create("10.8.1.12346")));
    when(sonar.getAllReleases(any())).thenReturn(releases);

    when(sonarQubeVersion.get()).thenReturn(parse("10.8.0.12345"));
    when(updateCenter.getSonar().getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenAvailableVersionsHaveDifferentNamingScheme_shouldReturnVersionIsActive() {
    TreeSet<Release> releases = new TreeSet<>();
    releases.add(new Release(sonar, Version.create("10.8.0.12345")));
    releases.add(new Release(sonar, Version.create("10.8.1.12346")));
    releases.add(new Release(sonar, Version.create("2025.1.0.12347")));
    when(sonar.getAllReleases(any())).thenReturn(releases);

    when(sonarQubeVersion.get()).thenReturn(parse("10.8.0.12345"));
    when(updateCenter.getSonar().getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }


  public static SortedSet<Release> getReleases() {
    TreeSet<Release> releases = new TreeSet<>();
    releases.add(new Release(sonar, Version.create("9.9")));
    releases.add(new Release(sonar, Version.create("9.9.1")));
    releases.add(new Release(sonar, Version.create("9.9.2")));
    releases.add(new Release(sonar, Version.create("9.9.3")));
    releases.add(new Release(sonar, Version.create("9.9.4")));
    releases.add(new Release(sonar, Version.create("10.0")));
    releases.add(new Release(sonar, Version.create("10.1")));
    releases.add(new Release(sonar, Version.create("10.2")));
    releases.add(new Release(sonar, Version.create("10.2.1")));
    releases.add(new Release(sonar, Version.create("10.3")));
    releases.add(new Release(sonar, Version.create("10.4")));
    releases.add(new Release(sonar, Version.create("10.4.1")));
    releases.add(new Release(sonar, Version.create("10.9.1")));
    releases.add(new Release(sonar, Version.create("10.10.1")));
    releases.add(new Release(sonar, Version.create("10.10.2")));
    releases.add(new Release(sonar, Version.create("10.10.3")));
    return releases;
  }

}
