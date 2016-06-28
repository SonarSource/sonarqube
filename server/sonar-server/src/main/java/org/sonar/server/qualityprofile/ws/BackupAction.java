/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.Response.Stream;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonarqube.ws.MediaTypes;

public class BackupAction implements QProfileWsAction {

  private final QProfileBackuper backuper;

  private final DbClient dbClient;

  private QProfileFactory profileFactory;

  private final Languages languages;

  public BackupAction(QProfileBackuper backuper, DbClient dbClient, QProfileFactory profileFactory, Languages languages) {
    this.backuper = backuper;
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction backup = controller.createAction("backup")
      .setSince("5.2")
      .setDescription("Backup a quality profile in XML form. The exported profile can be restored through api/qualityprofiles/restore.")
      .setResponseExample(getClass().getResource("backup-example.xml"))
      .setHandler(this);

    QProfileIdentificationParamUtils.defineProfileParams(backup, languages);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Stream stream = response.stream();
    stream.setMediaType(MediaTypes.XML);
    DbSession dbSession = dbClient.openSession(false);
    OutputStreamWriter writer = new OutputStreamWriter(stream.output(), StandardCharsets.UTF_8);
    try {
      String profileKey = QProfileIdentificationParamUtils.getProfileKeyFromParameters(request, profileFactory, dbSession);
      response.setHeader("Content-Disposition", String.format("attachment; filename=%s.xml", profileKey));
      backuper.backup(profileKey, writer);
    } finally {
      dbSession.close();
      IOUtils.closeQuietly(writer);
    }
  }

}
