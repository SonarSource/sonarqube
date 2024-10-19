/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.project.ws;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.NotFoundException;

@ServerSide
public class ProjectsWsSupport {

    public static final String PARAM_ORGANIZATION = "organization";

    private final DbClient dbClient;

    public ProjectsWsSupport(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    void addOrganizationParam(WebService.NewAction action) {
        action.createParam(PARAM_ORGANIZATION)
                .setDescription("The key of the organization")
                .setRequired(false)
                .setInternal(true);
    }

    OrganizationDto getOrganization(DbSession dbSession, String organizationKey) {
        return NotFoundException.checkFoundWithOptional(
                dbClient.organizationDao().selectByKey(dbSession, organizationKey),
                "No organization for key '%s'", organizationKey);
    }

}
