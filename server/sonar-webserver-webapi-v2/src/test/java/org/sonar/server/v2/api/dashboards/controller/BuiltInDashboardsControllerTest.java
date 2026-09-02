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
package org.sonar.server.v2.api.dashboards.controller;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.v2.api.ControllerTester;
import org.sonarsource.reporting.dashboards.BuiltInDashboard;
import org.sonarsource.reporting.dashboards.DashboardNotFoundException;
import org.sonarsource.reporting.dashboards.api.model.BuiltInDashboardItem;
import org.sonarsource.reporting.dashboards.api.model.DashboardResourceType;
import org.sonarsource.reporting.dashboards.server.BuiltInDashboardService;
import org.sonar.server.v2.telemetry.TelemetryBuiltInDashboardViewCountProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuiltInDashboardsControllerTest {
  private final BuiltInDashboardService builtInDashboardService = mock();
  private final PlatformEditionProvider editionProvider = mock();
  private final TelemetryBuiltInDashboardViewCountProvider builtInDashboardViewCountProvider = mock();
  private final BuiltInDashboardsController underTest = new BuiltInDashboardsController(builtInDashboardService, editionProvider,
    builtInDashboardViewCountProvider);
  private final MockMvc mockMvc = ControllerTester.getMockMvc(underTest);

  @Test
  void listBuiltInDashboards_inCommunity_returnsOnlyProjectHealth() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health")).thenReturn(projectHealth());

    var response = underTest.listBuiltInDashboards(null, null, 1, 50).getBody();

    assertThat(response.getDashboards()).extracting(BuiltInDashboardItem::getKey).containsExactly("project-health");
    assertThat(response.getPage().getTotal()).isOne();
    verify(builtInDashboardService, never()).list(any(), any(), anyInt(), anyInt());
  }

  @Test
  void listBuiltInDashboards_overHttpWithoutPagination_usesGeneratedDefaults() throws Exception {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health")).thenReturn(projectHealth());

    MvcResult result = mockMvc.perform(get("/dashboards/built-ins"))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getContentAsString()).contains("\"pageIndex\":1", "\"pageSize\":50");
  }

  @Test
  void listBuiltInDashboards_inCommunity_withNonProjectResourceType_returnsEmptyPageWithoutLookup() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));

    var response = underTest.listBuiltInDashboards(null,
      DashboardResourceType.PORTFOLIO, 1, 50).getBody();

    assertThat(response.getDashboards()).isEmpty();
    assertThat(response.getPage().getTotal()).isZero();
    verify(builtInDashboardService, never()).findByKey(any());
  }

  @Test
  void listBuiltInDashboards_inCommunity_whenProjectHealthIsUnavailable_returnsEmptyPage() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health"))
      .thenThrow(new DashboardNotFoundException("Built-in dashboard not found with key: project-health"));

    var response = underTest.listBuiltInDashboards(null, null, 1, 50).getBody();

    assertThat(response.getDashboards()).isEmpty();
    assertThat(response.getPage().getTotal()).isZero();
  }

  @Test
  void listBuiltInDashboards_inCommunity_matchesProjectHealthNameIgnoringCase() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health")).thenReturn(projectHealth());

    var response = underTest.listBuiltInDashboards("PROJECT HEALTH", null, 1, 50).getBody();

    assertThat(response.getDashboards()).extracting(BuiltInDashboardItem::getKey).containsExactly("project-health");
    assertThat(response.getPage().getTotal()).isOne();
  }

  @Test
  void listBuiltInDashboards_inCommunity_withNonMatchingQuery_returnsEmptyPage() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health")).thenReturn(projectHealth());

    var response = underTest.listBuiltInDashboards("portfolio", null, 1, 50).getBody();

    assertThat(response.getDashboards()).isEmpty();
    assertThat(response.getPage().getTotal()).isZero();
  }

  @Test
  void listBuiltInDashboards_inCommunity_onSecondPage_returnsEmptyDashboardListWithTotal() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health")).thenReturn(projectHealth());

    var response = underTest.listBuiltInDashboards(null, null, 2, 50).getBody();

    assertThat(response.getDashboards()).isEmpty();
    assertThat(response.getPage().getTotal()).isOne();
  }

  @Test
  void getBuiltInDashboard_inCommunity_returnsNotFoundForPaidDashboard() throws Exception {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));

    mockMvc.perform(get("/dashboards/built-ins/security-overview"))
      .andExpect(status().isNotFound());

    verify(builtInDashboardService, never()).findByKey(any());
  }

  @Test
  void getBuiltInDashboard_returnsDashboard() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health")).thenReturn(projectHealth());

    var response = underTest.getBuiltInDashboard("project-health");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody().getKey()).isEqualTo("project-health");
    verify(builtInDashboardService).findByKey("project-health");
    verify(builtInDashboardViewCountProvider).incrementCount();
  }

  @Test
  void getBuiltInDashboard_inEnterprise_returnsPaidDashboard() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.ENTERPRISE));
    when(builtInDashboardService.findByKey("security-overview")).thenReturn(securityOverview());

    var response = underTest.getBuiltInDashboard("security-overview");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody().getKey()).isEqualTo("security-overview");
    verify(builtInDashboardService).findByKey("security-overview");
  }

  @Test
  void getBuiltInDashboard_whenDashboardDoesNotExist_returnsNotFound() throws Exception {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(builtInDashboardService.findByKey("project-health"))
      .thenThrow(new DashboardNotFoundException("Built-in dashboard not found with key: project-health"));

    mockMvc.perform(get("/dashboards/built-ins/project-health"))
      .andExpect(status().isNotFound());

    verify(builtInDashboardViewCountProvider, never()).incrementCount();
  }

  @Test
  void listBuiltInDashboards_inEnterprise_returnsAllBuiltIns() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.ENTERPRISE));
    when(builtInDashboardService.list(null, null, 1, 50)).thenReturn(List.of(projectHealth()));
    when(builtInDashboardService.count(null, null)).thenReturn(1);

    var response = underTest.listBuiltInDashboards(null, null, 1, 50).getBody();

    assertThat(response.getDashboards()).extracting(BuiltInDashboardItem::getKey).containsExactly("project-health");
    verify(builtInDashboardService).list(null, null, 1, 50);
  }

  private static BuiltInDashboard projectHealth() {
    return new BuiltInDashboard("project-health", "Project Health", "Description", 123L, org.sonarsource.reporting.dashboards.DashboardResourceType.PROJECT, "{}");
  }

  private static BuiltInDashboard securityOverview() {
    return new BuiltInDashboard("security-overview", "Security Overview", "Description", 123L, org.sonarsource.reporting.dashboards.DashboardResourceType.PROJECT, "{}");
  }
}
