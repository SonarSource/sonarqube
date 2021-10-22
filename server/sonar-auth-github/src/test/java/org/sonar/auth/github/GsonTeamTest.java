/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonTeamTest {

  @Test
  public void parse_one_team() {
    List<GsonTeam> underTest = GsonTeam.parse(
      "[\n" +
        "  {\n" +
        "    \"name\": \"Developers\",\n" +
        "    \"slug\": \"developers\",\n" +
        "    \"organization\": {\n" +
        "      \"login\": \"SonarSource\"\n" +
        "    }\n" +
        "  }\n" +
        "]");
    assertThat(underTest).hasSize(1);

    assertThat(underTest.get(0).getId()).isEqualTo("developers");
    assertThat(underTest.get(0).getOrganizationId()).isEqualTo("SonarSource");
  }

  @Test
  public void parse_two_teams() {
    List<GsonTeam> underTest = GsonTeam.parse(
      "[\n" +
        "  {\n" +
        "    \"name\": \"Developers\",\n" +
        "    \"slug\": \"developers\",\n" +
        "    \"organization\": {\n" +
        "      \"login\": \"SonarSource\"\n" +
        "    }\n" +
        "  },\n" +
        "  {\n" +
        "    \"login\": \"SonarSource Developers\",\n" +
        "    \"organization\": {\n" +
        "      \"login\": \"SonarQubeCommunity\"\n" +
        "    }\n" +
        "  }\n" +
        "]");
    assertThat(underTest).hasSize(2);
  }

  @Test
  public void should_have_no_arg_constructor() {
    assertThat(new GsonTeam().getId()).isEmpty();
    assertThat(new GsonTeam.GsonOrganization().getLogin()).isEmpty();
  }

}
