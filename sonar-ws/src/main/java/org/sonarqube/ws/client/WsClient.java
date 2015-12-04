/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.ws.client;

import org.sonarqube.ws.client.component.ComponentsService;
import org.sonarqube.ws.client.issue.IssuesService;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.qualitygate.QualityGatesService;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;
import org.sonarqube.ws.client.usertoken.UserTokensService;

/**
 * @since 5.3
 */
public interface WsClient {
  ComponentsService components();

  IssuesService issues();

  PermissionsService permissions();

  QualityProfilesService qualityProfiles();

  UserTokensService userTokens();

  QualityGatesService qualityGates();

  WsConnector wsConnector();
}
