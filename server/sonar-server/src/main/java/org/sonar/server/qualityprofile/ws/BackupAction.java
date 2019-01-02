/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import java.io.OutputStreamWriter;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.Response.Stream;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonarqube.ws.MediaTypes;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BackupAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileBackuper backuper;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;

  public BackupAction(DbClient dbClient, QProfileBackuper backuper, QProfileWsSupport wsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.backuper = backuper;
    this.wsSupport = wsSupport;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("backup")
      .setSince("5.2")
      .setDescription("Backup a quality profile in XML form. The exported profile can be restored through api/qualityprofiles/restore.")
      .setResponseExample(getClass().getResource("backup-example.xml"))
      .setHandler(this);

    QProfileReference.defineParams(action, languages);

    QProfileWsSupport.createOrganizationParam(action).setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // Allowed to users without admin permission: http://jira.sonarsource.com/browse/SONAR-2039
    Stream stream = response.stream();
    stream.setMediaType(MediaTypes.XML);
    try (OutputStreamWriter writer = new OutputStreamWriter(stream.output(), UTF_8);
      DbSession dbSession = dbClient.openSession(false)) {

      QProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.from(request));
      response.setHeader("Content-Disposition", String.format("attachment; filename=%s.xml", profile.getKee()));
      backuper.backup(dbSession, profile, writer);
    }
  }
}
