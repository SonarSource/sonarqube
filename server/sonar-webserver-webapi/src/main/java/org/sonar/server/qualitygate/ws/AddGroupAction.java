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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualitygate.QualityGateGroupPermissionsDto;
import org.sonar.db.user.GroupDto;

import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_ADD_GROUP;

public class AddGroupAction extends AbstractGroupAction {
  private final UuidFactory uuidFactory;

  public AddGroupAction(DbClient dbClient, UuidFactory uuidFactory, QualityGatesWsSupport wsSupport) {
    super(dbClient, wsSupport);
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_ADD_GROUP)
      .setDescription("Allow a group of users to edit a Quality Gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Gates'</li>" +
        "  <li>Edit right on the specified quality gate</li>" +
        "</ul>")
      .setHandler(this)
      .setPost(true)
      .setSince("9.2");

    super.defineGateAndGroupParameters(action);
  }

  @Override
  protected void apply(DbSession dbSession, QualityGateDto qualityGate, GroupDto group) {
    if (dbClient.qualityGateGroupPermissionsDao().exists(dbSession, qualityGate, group)) {
      return;
    }
    dbClient.qualityGateGroupPermissionsDao().insert(dbSession,
      new QualityGateGroupPermissionsDto()
        .setUuid(uuidFactory.create())
        .setGroupUuid(group.getUuid())
        .setQualityGateUuid(qualityGate.getUuid()),
      qualityGate.getName(),
      group.getName());
    dbSession.commit();
  }

}
