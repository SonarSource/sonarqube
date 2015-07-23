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

package org.sonar.server.metric.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.server.db.DbClient;

import java.util.List;

public class DomainsAction implements MetricsWsAction {

  private final DbClient dbClient;

  public DomainsAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("domains")
      .setDescription("List all custom metric domains.")
      .setSince("5.2")
      .setResponseExample(getClass().getResource("example-domains.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<String> domains = dbClient.metricDao().selectEnabledDomains(dbSession);
      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      writeDomains(json, domains);
      json.endObject();
      json.close();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private static void writeDomains(JsonWriter json, List<String> domains) {
    json.name("domains");
    json.beginArray();
    json.values(domains);
    json.endArray();
  }
}
