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

import org.sonarsource.reporting.dashboards.BuiltInDashboard;
import org.sonarsource.reporting.dashboards.DashboardNotFoundException;
import org.sonarsource.reporting.dashboards.api.model.BuiltInDashboardResponse;
import org.sonarsource.reporting.dashboards.api.model.BuiltInDashboardsResponse;
import org.sonarsource.reporting.dashboards.api.model.DashboardPage;
import org.sonarsource.reporting.dashboards.api.model.DashboardResourceType;
import org.sonarsource.reporting.dashboards.api.model.DashboardsModelConverter;
import org.sonarsource.reporting.dashboards.api.rest.BuiltInDashboardsApi;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarsource.reporting.dashboards.server.BuiltInDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves built-in dashboards, with paid dashboards restricted to paid editions. */
@RestController
@RequestMapping("/dashboards/built-ins")
public class BuiltInDashboardsController implements BuiltInDashboardsApi {
  private static final String PROJECT_HEALTH_KEY = "project-health";

  private final BuiltInDashboardService builtInDashboardService;
  private final PlatformEditionProvider editionProvider;

  public BuiltInDashboardsController(BuiltInDashboardService builtInDashboardService, PlatformEditionProvider editionProvider) {
    this.builtInDashboardService = builtInDashboardService;
    this.editionProvider = editionProvider;
  }

  @GetMapping
  @Override
  public ResponseEntity<BuiltInDashboardsResponse> listBuiltInDashboards(
    @Nullable String q,
    @Nullable DashboardResourceType resourceType,
    Integer pageIndex,
    Integer pageSize) {
    if (hasPaidBuiltIns()) {
      return listAllBuiltIns(q, resourceType, pageIndex, pageSize);
    }
    return listProjectHealth(q, resourceType, pageIndex, pageSize);
  }

  @GetMapping("/{key}")
  @Override
  public ResponseEntity<BuiltInDashboardResponse> getBuiltInDashboard(@PathVariable("key") String key) {
    if (!hasPaidBuiltIns() && !PROJECT_HEALTH_KEY.equals(key)) {
      throw new NotFoundException("Built-in dashboard not found with key: " + key);
    }
    try {
      return ResponseEntity.ok(DashboardsModelConverter.toApiBuiltInDashboardResponse(builtInDashboardService.findByKey(key)));
    } catch (DashboardNotFoundException e) {
      throw new NotFoundException(e.getMessage());
    }
  }

  private ResponseEntity<BuiltInDashboardsResponse> listAllBuiltIns(@Nullable String q, @Nullable DashboardResourceType resourceType,
    Integer pageIndex, Integer pageSize) {
    org.sonarsource.reporting.dashboards.DashboardResourceType coreResourceType = resourceType == null
      ? null : DashboardsModelConverter.toCoreResourceType(resourceType);
    List<BuiltInDashboard> dashboards = pageSize > 0 ? builtInDashboardService.list(q, coreResourceType, pageIndex, pageSize) : List.of();
    int total = builtInDashboardService.count(q, coreResourceType);
    return ResponseEntity.ok(new BuiltInDashboardsResponse(dashboards.stream().map(DashboardsModelConverter::toApiBuiltInDashboardItem).toList(),
      new DashboardPage(pageIndex, pageSize, total)));
  }

  private ResponseEntity<BuiltInDashboardsResponse> listProjectHealth(@Nullable String q, @Nullable DashboardResourceType resourceType,
    Integer pageIndex, Integer pageSize) {
    if (resourceType != null && resourceType != DashboardResourceType.PROJECT) {
      return emptyPage(pageIndex, pageSize);
    }
    BuiltInDashboard dashboard;
    try {
      dashboard = builtInDashboardService.findByKey(PROJECT_HEALTH_KEY);
    } catch (DashboardNotFoundException e) {
      return emptyPage(pageIndex, pageSize);
    }
    boolean matches = matchesQuery(dashboard, q);
    List<BuiltInDashboard> dashboards = matches && pageSize > 0 && pageIndex == 1 ? List.of(dashboard) : List.of();
    return ResponseEntity.ok(new BuiltInDashboardsResponse(dashboards.stream().map(DashboardsModelConverter::toApiBuiltInDashboardItem).toList(),
      new DashboardPage(pageIndex, pageSize, matches ? 1 : 0)));
  }

  private static ResponseEntity<BuiltInDashboardsResponse> emptyPage(Integer pageIndex, Integer pageSize) {
    return ResponseEntity.ok(new BuiltInDashboardsResponse(List.of(), new DashboardPage(pageIndex, pageSize, 0)));
  }

  private boolean hasPaidBuiltIns() {
    return editionProvider.get().filter(edition -> edition == Edition.ENTERPRISE || edition == Edition.DATACENTER).isPresent();
  }

  private static boolean matchesQuery(BuiltInDashboard dashboard, @Nullable String q) {
    if (q == null || q.isBlank()) {
      return true;
    }
    String normalizedQuery = q.toLowerCase(Locale.ROOT);
    return dashboard.key().toLowerCase(Locale.ROOT).contains(normalizedQuery)
      || dashboard.name().toLowerCase(Locale.ROOT).contains(normalizedQuery);
  }
}
