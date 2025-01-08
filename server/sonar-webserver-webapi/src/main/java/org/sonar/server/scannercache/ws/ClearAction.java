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
package org.sonar.server.scannercache.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.scannercache.ScannerCache;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class ClearAction implements AnalysisCacheWsAction {
  public static final String PARAM_PROJECT_KEY = "project";
  public static final String PARAM_BRANCH_KEY = "branch";
  private final UserSession userSession;
  private final ScannerCache cache;

  public ClearAction(UserSession userSession, ScannerCache cache) {
    this.userSession = userSession;
    this.cache = cache;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction clearDefinition = context.createAction("clear")
      .setInternal(true)
      .setPost(true)
      .setDescription("Clear all or part of the scanner's cached data. Requires global administration permission.")
      .setSince("9.4")
      .setHandler(this);
    clearDefinition.createParam(PARAM_PROJECT_KEY)
      .setRequired(false)
      .setSince("9.7")
      .setDescription("Filter which project's cached data will be cleared with the provided key.")
      .setExampleValue("org.sonarsource.sonarqube:sonarqube-private");
    clearDefinition.createParam(PARAM_BRANCH_KEY)
      .setRequired(false)
      .setSince("9.7")
      .setDescription("Filter which project's branch's cached data will be cleared with the provided key. '" + PARAM_PROJECT_KEY + "' parameter must be set.")
      .setExampleValue("6468");
  }

  private static class ClearRequestDto {
    private final String projectKey;
    private final String branchKey;

    private ClearRequestDto(Request request) {
      this.projectKey = request.param(PARAM_PROJECT_KEY);
      this.branchKey = request.param(PARAM_BRANCH_KEY);
    }
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    checkPermission();
    ClearRequestDto params = new ClearRequestDto(request);
    validateParams(params);
    if (params.projectKey != null) {
      if (params.branchKey != null) {
        cache.clearBranch(params.projectKey, params.branchKey);
      } else {
        cache.clearProject(params.projectKey);
      }
    } else {
      cache.clear();
    }
    response.noContent();
  }

  private static void validateParams(ClearRequestDto params) {
    if (params.branchKey != null) {
      checkArgument(
        params.projectKey != null,
        "{} needs to be specified when {} is present",
        PARAM_PROJECT_KEY, PARAM_BRANCH_KEY);
    }
  }

  private void checkPermission() {
    if (!userSession.hasPermission(GlobalPermission.ADMINISTER)) {
      throw insufficientPrivilegesException();
    }
  }
}
