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

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.CreateQuery;
import org.sonar.wsclient.services.DeleteQuery;
import org.sonar.wsclient.services.Query;

import java.io.IOException;

/**
 * @since 2.1
 */
public class HttpClient4Connector extends Connector {

  private Host server;

  public HttpClient4Connector(Host server) {
    this.server = server;
  }

  public String execute(Query<?> query) {
    return executeRequest(newGetMethod(query));
  }

  public String execute(CreateQuery<?> query) {
    return executeRequest(newPostMethod(query));
  }

  public String execute(DeleteQuery<?> query) {
    return executeRequest(newDeleteMethod(query));
  }

  private String executeRequest(HttpRequestBase request) {
    String json = null;
    DefaultHttpClient client = createClient();
    try {
      BasicHttpContext context = createLocalContext(client);
      HttpResponse response = client.execute(request, context);
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          json = EntityUtils.toString(entity);

        } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND) {
          throw new ConnectionException("HTTP error: " + response.getStatusLine().getStatusCode() + ", msg: " + response.getStatusLine().getReasonPhrase() + ", query: " + request.toString());
        }
      }

    } catch (IOException e) {
      throw new ConnectionException("Query: " + request.getURI(), e);

    } finally {
      client.getConnectionManager().shutdown();
    }
    return json;
  }

  private DefaultHttpClient createClient() {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpParams params = client.getParams();
    HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MS);
    HttpConnectionParams.setSoTimeout(params, TIMEOUT_MS);
    if (server.getUsername() != null) {
      client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(server.getUsername(), server.getPassword()));
    }
    return client;
  }

  private BasicHttpContext createLocalContext(DefaultHttpClient client) {
    BasicHttpContext localcontext = new BasicHttpContext();

    if (server.getUsername() != null) {
      // Generate BASIC scheme object and stick it to the local
      // execution context
      BasicScheme basicAuth = new BasicScheme();
      localcontext.setAttribute("preemptive-auth", basicAuth);

      // Add as the first request interceptor
      client.addRequestInterceptor(new PreemptiveAuth(), 0);
    }
    return localcontext;
  }

  private HttpGet newGetMethod(Query<?> query) {
    HttpGet get = new HttpGet(server.getHost() + query.getUrl());
    setJsonHeader(get);
    return get;
  }

  private HttpDelete newDeleteMethod(DeleteQuery<?> query) {
    HttpDelete delete = new HttpDelete(server.getHost() + query.getUrl());
    setJsonHeader(delete);
    return delete;
  }

  private HttpPost newPostMethod(CreateQuery<?> query) {
    HttpPost post = new HttpPost(server.getHost() + query.getUrl());
    setJsonHeader(post);
    return post;
  }

  private void setJsonHeader(HttpRequestBase request) {
    request.setHeader("Accept", "application/json");
  }

  static final class PreemptiveAuth implements HttpRequestInterceptor {
    public void process(
        final HttpRequest request,
        final HttpContext context) throws HttpException {

      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

      // If no auth scheme available yet, try to initialize it preemptively
      if (authState.getAuthScheme() == null) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
        HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        if (authScheme != null) {
          Credentials creds = credsProvider.getCredentials(
              new AuthScope(
                  targetHost.getHostName(),
                  targetHost.getPort()));
          if (creds == null) {
            throw new HttpException("No credentials for preemptive authentication");
          }
          authState.setAuthScheme(authScheme);
          authState.setCredentials(creds);
        }
      }
    }
  }
}
