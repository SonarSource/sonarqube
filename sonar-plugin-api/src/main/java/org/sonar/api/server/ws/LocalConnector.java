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
package org.sonar.api.server.ws;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;

/**
 * This class allows a web service to call another web service through the sonar-ws library.
 * The call is in-process, synchronous and does not involve the HTTP stack.
 * <p>
 * Example of a web service that loads some issues:
 * <pre>
 * import org.sonar.api.server.ws.RequestHandler;
 * import org.sonarqube.ws.client.WsClientFactories;
 *
 * public class MyRequestHandler implements RequestHandler {
 *   {@literal @}Override
 *   public void handle(Request request, Response response) {
 *     WsClient wsClient = WsClientFactories.getLocal().newClient(request.localConnector());
 *     SearchWsResponse issues = wsClient.issues().search(new SearchWsRequest());
 *     // ...
 *   }
 * }
 * </pre>
 * <p>
 * It requires to use the sonar-ws library which Maven ids are:
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.sonarsource.sonarqube&lt;/groupId&gt;
 *     &lt;artifactId&gt;sonar-ws&lt;/artifactId&gt;
 *   &lt;/dependency&gt;
 * </pre>
 *
 * @since 5.5
 */
public interface LocalConnector {

  LocalResponse call(LocalRequest request);

  interface LocalRequest {
    /**
     * URL path, which is the concatenation of controller path and action key, for example "api/issues/search"
     *
     * @see org.sonar.api.server.ws.WebService.Controller#path
     * @see org.sonar.api.server.ws.WebService.Action#key
     */
    String getPath();

    /**
     * @see Request#getMediaType()
     */
    String getMediaType();

    /**
     * HTTP method. Possible values are "GET" and "POST"
     *
     * @see Request#method()
     */
    String getMethod();

    /**
     * @see Request#hasParam(String)
     */
    boolean hasParam(String key);

    /**
     * @see Request#param(String)
     */
    @CheckForNull
    String getParam(String key);

    /**
     * @see Request#multiParam(String)
     */
    List<String> getMultiParam(String key);

    /**
     * @see Request#header(String)
     * @since 6.6
     */
    Optional<String> getHeader(String name);

    Map<String,String[]> getParameterMap();
  }

  interface LocalResponse {
    /**
     * @see org.sonar.api.server.ws.Response.Stream#setStatus(int)
     */
    int getStatus();

    /**
     * @see org.sonar.api.server.ws.Response.Stream#setMediaType(String)
     */
    String getMediaType();

    /**
     * Response body
     */
    byte[] getBytes();

    /**
     * HTTP headers
     *
     * @see Response#setHeader(String, String)
     */
    Collection<String> getHeaderNames();

    /**
     * @see Response#setHeader(String, String)
     */
    @CheckForNull
    String getHeader(String name);
  }

}
