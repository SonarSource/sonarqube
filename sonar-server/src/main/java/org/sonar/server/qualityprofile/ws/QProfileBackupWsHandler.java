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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualityprofile.QProfileBackup;
import org.sonar.server.qualityprofile.QProfileResult;

public class QProfileBackupWsHandler implements RequestHandler {

  private final QProfileBackup qProfileBackup;

  public QProfileBackupWsHandler(QProfileBackup qProfileBackup) {
    this.qProfileBackup = qProfileBackup;
  }

  @Override
  public void handle(Request request, Response response) {
    final String language = request.mandatoryParam("language");
    QProfileResult result = qProfileBackup.restoreDefaultProfilesByLanguage(language);

    if (!result.infos().isEmpty() || !result.warnings().isEmpty()) {
      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      if (!result.infos().isEmpty()) {
        json.name("infos").beginArray();
        for (String info : result.infos()) {
          json.value(info);
        }
        json.endArray();
      }
      if (!result.warnings().isEmpty()) {
        json.name("warnings").beginArray();
        for (String warning : result.warnings()) {
          json.value(warning);
        }
        json.endArray();
      }
      json.endObject().close();
    } else {
      response.noContent();
    }
  }

}
