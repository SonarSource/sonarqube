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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.LocalWsClientFactory;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;

public final class LocalCallWebService implements WebService {

  private final LocalWsClientFactory wsClientFactory;

  public LocalCallWebService(LocalWsClientFactory wsClientFactory) {
    this.wsClientFactory = wsClientFactory;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("local_ws_call");
    controller.createAction("protobuf_data").setHandler(new ProtobufHandler());
    controller.createAction("json_data").setHandler(new JsonHandler());
    controller.createAction("require_permission").setHandler(new RequirePermissionHandler());
    controller.done();
  }

  private class ProtobufHandler implements RequestHandler {
    @Override
    public void handle(Request request, Response response) {
      WsClient client = wsClientFactory.newClient(request.localConnector());

      Ce.TaskTypesWsResponse ceTaskTypes = client.ce().taskTypes();
      response.stream().setStatus(ceTaskTypes.getTaskTypesCount() > 0 ? 200 : 500);
    }
  }

  private class JsonHandler implements RequestHandler {
    @Override
    public void handle(Request request, Response response) {
      WsClient client = wsClientFactory.newClient(request.localConnector());

      WsResponse jsonResponse = client.wsConnector().call(new GetRequest("api/issues/search"));
      boolean ok = jsonResponse.contentType().equals(MediaTypes.JSON)
        && jsonResponse.isSuccessful()
        && jsonResponse.content().contains("\"issues\":");
      response.stream().setStatus(ok ? 200 : 500);
    }
  }

  private class RequirePermissionHandler implements RequestHandler {
    @Override
    public void handle(Request request, Response response) {
      WsClient client = wsClientFactory.newClient(request.localConnector());

      WsResponse jsonResponse = client.wsConnector().call(new GetRequest("api/system/info"));

      boolean ok = MediaTypes.JSON.equals(jsonResponse.contentType())
        && jsonResponse.isSuccessful()
        && jsonResponse.content().startsWith("{");
      response.stream().setStatus(ok ? 200 : 500);
    }
  }
}
