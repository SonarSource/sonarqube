/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Calendar;
import java.util.Collections;
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
  void evaluateIfActiveVersion_whenInstalledVersionIsPastLtaAndWithinSixMonthFromLta_shouldReturnVersionIsActive() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MONTH, -5);
    calendar.set(Calendar.DAY_OF_MONTH, 1);

    when(sonarQubeVersion.get()).thenReturn(parse("8.9.5"));
    SortedSet<Release> releases = getReleases();
    releases.stream().filter(r -> r.getVersion().equals(Version.create("9.9"))).findFirst().get().setDate(calendar.getTime());
    when(sonar.getAllReleases(any())).thenReturn(releases);

    assertThat(underTest.evaluateIfActiveVersion(updateCenter)).isTrue();
  }

  @Test
  void evaluateIfActiveVersion_whenInstalledVersionIsPastLtaAndAfterSixMonthFromLta_shouldReturnVersionNotActive() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MONTH, -7);
    calendar.set(Calendar.DAY_OF_MONTH, 1);

    when(system2.now()).thenCallRealMethod();

    when(sonarQubeVersion.get()).thenReturn(parse("8.9.5"));
    SortedSet<Release> releases = getReleases();
    releases.stream().filter(r -> r.getVersion().equals(Version.create("9.9"))).findFirst().get().setDate(calendar.getTime());
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
