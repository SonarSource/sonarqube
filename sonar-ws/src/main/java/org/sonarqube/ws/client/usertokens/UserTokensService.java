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
package org.sonarqube.ws.client.usertokens;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.UserTokens.GenerateWsResponse;
import org.sonarqube.ws.UserTokens.SearchWsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_tokens">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class UserTokensService extends BaseService {

  public UserTokensService(WsConnector wsConnector) {
    super(wsConnector, "api/user_tokens");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_tokens/generate">Further information about this action online (including a response example)</a>
   * @since 5.3
   */
  public GenerateWsResponse generate(GenerateRequest request) {
    return call(
      new PostRequest(path("generate"))
        .setParam("login", request.getLogin())
        .setParam("name", request.getName()),
      GenerateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_tokens/revoke">Further information about this action online (including a response example)</a>
   * @since 5.3
   */
  public void revoke(RevokeRequest request) {
    call(
      new PostRequest(path("revoke"))
        .setParam("login", request.getLogin())
        .setParam("name", request.getName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_tokens/search">Further information about this action online (including a response example)</a>
   * @since 5.3
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("login", request.getLogin()),
      SearchWsResponse.parser());
  }
}
