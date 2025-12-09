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
package org.sonar.auth.github;

import java.util.Optional;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubTeamConverterTest {

  @Test
  public void toGroupName_withGsonTeam_returnsCorrectGroupName() {
    GsonTeam team = new GsonTeam("team-1", new GsonTeam.GsonOrganization("Org1"));
    assertThat(GithubTeamConverter.toGroupName(team)).isEqualTo("Org1/team-1");
  }

  @Test
  public void toGroupName_withGroupAndName_returnsCorrectGroupName() {
    assertThat(GithubTeamConverter.toGroupName("Org1", "team-1")).isEqualTo("Org1/team-1");
  }

  @Test
  public void extractOrganizationName_whenNameIsCorrect_extractsOrganizationName() {
    assertThat(GithubTeamConverter.extractOrganizationName("Org1/team1")).isEqualTo(Optional.of("Org1"));
    assertThat(GithubTeamConverter.extractOrganizationName("Org1/team1/team2")).isEqualTo(Optional.of("Org1"));
  }

  @Test
  public void extractOrganizationName_whenNameIsIncorrect_returnEmpty() {
    assertThat(GithubTeamConverter.extractOrganizationName("Org1")).isEmpty();
    assertThat(GithubTeamConverter.extractOrganizationName("Org1/")).isEmpty();
  }

  @Test
  public void extractTeamName_whenNameIsCorrect_extractsTeamName() {
    assertThat(GithubTeamConverter.extractTeamName("Org1/team1")).isEqualTo(Optional.of("team1"));
    assertThat(GithubTeamConverter.extractTeamName("Org1/team1/team2")).isEqualTo(Optional.of("team1/team2"));
  }

  @Test
  public void extractTeamName_whenNameIsIncorrect_returnEmpty() {
    assertThat(GithubTeamConverter.extractTeamName("Org1")).isEmpty();
    assertThat(GithubTeamConverter.extractTeamName("Org1/")).isEmpty();
  }

}
