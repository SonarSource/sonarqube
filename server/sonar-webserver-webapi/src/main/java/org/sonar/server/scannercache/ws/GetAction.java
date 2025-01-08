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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.scannercache.ScannerCache;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class GetAction implements AnalysisCacheWsAction {
  private static final String PROJECT = "project";
  private static final String BRANCH = "branch";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final ScannerCache cache;

  public GetAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, ScannerCache cache) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.cache = cache;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("get")
      .setDescription("Get the scanner's cached data for a branch. Requires scan permission on the project. "
        + "Data is returned gzipped if the corresponding 'Accept-Encoding' header is set in the request.")
      .setChangelog(new Change("9.9", "The web service is no longer internal"))
      .setSince("9.4")
      .setContentType(Response.ContentType.BINARY)
      .setHandler(this);

    action.createParam(PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);

    action.createParam(BRANCH)
      .setDescription("Branch key. If not provided, main branch will be used.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setRequired(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PROJECT);
    String branchKey = request.param(BRANCH);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
      checkPermission(project);
      BranchDto branchDto = componentFinder.getBranchOrPullRequest(dbSession, project, branchKey, null);

      try (DbInputStream dbInputStream = cache.get(branchDto.getUuid())) {
        if (dbInputStream == null) {
          throw new NotFoundException("No cache for given branch or pull request");
        }

        boolean compressed = requestedCompressedData(request);
        try (OutputStream output = response.stream().output()) {
          if (compressed) {
            response.setHeader("Content-Encoding", "gzip");
            // data is stored compressed
            IOUtils.copy(dbInputStream, output);
          } else {
            try (InputStream uncompressedInput = new GZIPInputStream(dbInputStream)) {
              IOUtils.copy(uncompressedInput, output);
            }
          }
        }
      }
    }
  }

  private static boolean requestedCompressedData(Request request) {
    return request.header("Accept-Encoding")
      .map(encoding -> Arrays.stream(encoding.split(","))
        .map(String::trim)
        .anyMatch("gzip"::equals))
      .orElse(false);
  }

  private void checkPermission(ProjectDto project) {
    if (userSession.hasEntityPermission(UserRole.SCAN, project) ||
      userSession.hasEntityPermission(UserRole.ADMIN, project) ||
      userSession.hasPermission(SCAN)) {
      return;
    }
    throw insufficientPrivilegesException();
  }
}
