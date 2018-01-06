/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.ws.WebServiceEngine;

class BulkChangeWsResponse {

  private BulkChangeWsResponse() {
    // use static methods
  }

  static void writeResponse(BulkChangeResult result, Response response) {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      json.prop("succeeded", result.countSucceeded());
      json.prop("failed", result.countFailed());
      WebServiceEngine.writeErrors(json, result.getErrors());
      json.endObject();
    }
  }
}
