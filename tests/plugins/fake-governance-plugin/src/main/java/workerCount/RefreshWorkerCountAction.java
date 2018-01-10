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
package workerCount;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.ce.ws.CeWsAction;

import static workerCount.FakeWorkerCountProviderImpl.PROPERTY_WORKER_COUNT;

@ServerSide
public class RefreshWorkerCountAction implements CeWsAction {
  private static final String PARAM_COUNT = "count";

  private final CeHttpClient ceHttpClient;
  private final DbClient dbClient;

  public RefreshWorkerCountAction(CeHttpClient ceHttpClient, DbClient dbClient) {
    this.ceHttpClient = ceHttpClient;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("refreshWorkerCount")
      .setPost(true)
      .setHandler(this)
      .createParam(PARAM_COUNT)
      .setPossibleValues("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) {
    String count = request.getParam(PARAM_COUNT).getValue();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.propertiesDao().saveProperty(new PropertyDto()
        .setKey(PROPERTY_WORKER_COUNT)
        .setValue(count));
      dbSession.commit();
    }
    ceHttpClient.refreshCeWorkerCount();
  }
}
