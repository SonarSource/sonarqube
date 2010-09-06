/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.connectors;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.util.URIUtil;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.CreateQuery;
import org.sonar.wsclient.services.DeleteQuery;
import org.sonar.wsclient.services.Query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    params.setConnectionTimeout(TIMEOUT_MS);
    params.setSoTimeout(TIMEOUT_MS);
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

  public String execute(Query query) {
    return executeRequest(newGetRequest(query));
  }

  public String execute(CreateQuery query) {
    return executeRequest(newPostRequest(query));
  }

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

  private HttpMethodBase newGetRequest(Query query) {
    try {
      String url = server.getHost() + URIUtil.encodeQuery(query.getUrl());
      HttpMethodBase method = new GetMethod(url);
      method.setRequestHeader("Accept", "application/json");
      return method;

    } catch (URIException e) {
      throw new ConnectionException("Query: " + query, e);
    }
  }

  private HttpMethodBase newPostRequest(CreateQuery query) {
    try {
      String url = server.getHost() + URIUtil.encodeQuery(query.getUrl());
      HttpMethodBase method = new PostMethod(url);
      method.setRequestHeader("Accept", "application/json");
      return method;

    } catch (URIException e) {
      throw new ConnectionException("Query: " + query, e);
    }
  }

  private HttpMethodBase newDeleteRequest(DeleteQuery query) {
    try {
      String url = server.getHost() + URIUtil.encodeQuery(query.getUrl());
      HttpMethodBase method = new DeleteMethod(url);
      method.setRequestHeader("Accept", "application/json");
      return method;

    } catch (URIException e) {
      throw new ConnectionException("Query: " + query, e);
    }
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
          // TODO
        }
      }
    }
  }
}
