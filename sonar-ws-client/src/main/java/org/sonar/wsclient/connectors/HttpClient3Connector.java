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
package org.sonar.wsclient.connectors;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.*;

import java.io.*;

/**
 * @since 2.1
 */
public class HttpClient3Connector extends Connector {

  private static final int MAX_TOTAL_CONNECTIONS = 40;
  private static final int MAX_HOST_CONNECTIONS = 4;

  private final Host server;
  private HttpClient httpClient;

  public HttpClient3Connector(final Host server) {
    this.server = server;
    createClient();
  }

  public HttpClient3Connector(Host server, HttpClient httpClient) {
    this.httpClient = httpClient;
    this.server = server;
  }

  private void createClient() {
    final HttpConnectionManagerParams params = new HttpConnectionManagerParams();
    params.setConnectionTimeout(AbstractQuery.DEFAULT_TIMEOUT_MILLISECONDS);
    params.setSoTimeout(AbstractQuery.DEFAULT_TIMEOUT_MILLISECONDS);
    params.setDefaultMaxConnectionsPerHost(MAX_HOST_CONNECTIONS);
    params.setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
    final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    connectionManager.setParams(params);
    this.httpClient = new HttpClient(connectionManager);
    configureCredentials();
  }

  private void configureCredentials() {
    if (server.getUsername() != null) {
      httpClient.getParams().setAuthenticationPreemptive(true);
      Credentials defaultcreds = new UsernamePasswordCredentials(server.getUsername(), server.getPassword());
      httpClient.getState().setCredentials(AuthScope.ANY, defaultcreds);
    }
  }

  /**
   * @since 3.4
   */
  public HttpClient getHttpClient() {
    return httpClient;
  }

  @Override
  public String execute(Query<?> query) {
    return executeRequest(newGetRequest(query));
  }

  @Override
  public String execute(CreateQuery<?> query) {
    return executeRequest(newPostRequest(query));
  }

  @Override
  public String execute(UpdateQuery<?> query) {
    return executeRequest(newPutRequest(query));
  }

  @Override
  public String execute(DeleteQuery query) {
    return executeRequest(newDeleteRequest(query));
  }

  private String executeRequest(HttpMethodBase method) {
    String json = null;
    try {
      httpClient.executeMethod(method);

      if (method.getStatusCode() == HttpStatus.SC_OK) {
        json = getResponseBodyAsString(method);

      } else if (method.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
        throw new ConnectionException("HTTP error: " + method.getStatusCode() + ", msg: " + method.getStatusText() + ", query: " + method);
      }

    } catch (HttpException e) {
      throw new ConnectionException("Query: " + method, e);

    } catch (IOException e) {
      throw new ConnectionException("Query: " + method, e);

    } finally {
      if (method != null) {
        method.releaseConnection();
      }
    }
    return json;
  }

  private HttpMethodBase newGetRequest(Query<?> query) {
    HttpMethodBase method = new GetMethod(server.getHost() + query.getUrl());
    initRequest(method, query);
    return method;
  }

  private HttpMethodBase newDeleteRequest(DeleteQuery query) {
    HttpMethodBase method = new DeleteMethod(server.getHost() + query.getUrl());
    initRequest(method, query);
    return method;
  }

  private HttpMethodBase newPostRequest(CreateQuery<?> query) {
    PostMethod method = new PostMethod(server.getHost() + query.getUrl());
    initRequest(method, query);
    setRequestEntity(method, query);
    return method;
  }

  private HttpMethodBase newPutRequest(UpdateQuery<?> query) {
    PutMethod method = new PutMethod(server.getHost() + query.getUrl());
    initRequest(method, query);
    setRequestEntity(method, query);
    return method;
  }

  private void setRequestEntity(EntityEnclosingMethod request, AbstractQuery<?> query) {
    if (query.getBody() != null) {
      try {
        request.setRequestEntity(new StringRequestEntity(query.getBody(), "text/plain; charset=UTF-8", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new ConnectionException("Unsupported encoding", e);
      }
    }
  }

  private void initRequest(HttpMethodBase request, AbstractQuery query) {
    request.setRequestHeader("Accept", "application/json");
    if (query.getLocale() != null) {
      request.setRequestHeader("Accept-Language", query.getLocale());
    }
    request.getParams().setSoTimeout(query.getTimeoutMilliseconds());
  }

  private String getResponseBodyAsString(HttpMethod method) {
    BufferedReader reader = null;
    try {
      final InputStream inputStream = method.getResponseBodyAsStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
      final StringBuilder sb = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      return sb.toString();

    } catch (IOException e) {
      throw new ConnectionException("Can not read response", e);

    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (Exception e) {
          // wsclient does not have logging ability -> silently ignore
        }
      }
    }
  }
}
