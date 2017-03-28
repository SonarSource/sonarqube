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
package it.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityProfilesRestoreAndSearchTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void init() {
    orchestrator.resetData();
  }

  @Test
  public void restore_and_search_in_default_organization() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/authorisation/one-issue-per-line-profile.xml"));
    QualityProfiles.SearchWsResponse results = ItUtils.newAdminWsClient(orchestrator).qualityProfiles().search(new SearchWsRequest());
    assertThat(results.getProfilesList())
      .filteredOn(result -> "xoo".equals(result.getLanguage()))
      .filteredOn(result -> "one-issue-per-line".equals(result.getName()))
      .filteredOn(result -> "default-organization".equals(result.getOrganization()))
      .hasSize(1);
  }
}
