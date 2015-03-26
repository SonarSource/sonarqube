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
package org.sonar.server.qualityprofile.ws;

import com.google.common.base.Charsets;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.Response.Stream;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.qualityprofile.QProfileBackuper;

import java.io.OutputStreamWriter;

public class QProfileBackupAction implements BaseQProfileWsAction {

  private static final String PARAM_KEY = "key";
  private final QProfileBackuper backuper;

  public QProfileBackupAction(QProfileBackuper backuper) {
    this.backuper = backuper;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Stream stream = response.stream();
    stream.setMediaType("text/xml");
    backuper.backup(request.mandatoryParam(PARAM_KEY), new OutputStreamWriter(stream.output(), Charsets.UTF_8));
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("backup")
      .setSince("5.2")
      .setDescription("Backup a quality profile in XML form.")
      .setHandler(this)
      .createParam(PARAM_KEY)
        .setDescription("Key of the profile to backup.");
  }

}
