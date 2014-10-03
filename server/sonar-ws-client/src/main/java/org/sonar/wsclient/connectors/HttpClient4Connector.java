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

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.AbstractQuery;
import org.sonar.wsclient.services.CreateQuery;
import org.sonar.wsclient.services.DeleteQuery;
import org.sonar.wsclient.services.Query;
import org.sonar.wsclient.services.UpdateQuery;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @since 2.1
 */
public class HttpClient4Connector extends Connector {

  private Host server;
  private DefaultHttpClient client;

  public HttpClient4Connector(Host server) {
    this.server = server;
    initClient();
  }

  public DefaultHttpClient getHttpClient() {
    return client;
  }

  @Override
  public String execute(Query<?> query) {
    return executeRequest(newGetMethod(query));
  }

  @Override
  public String execute(CreateQuery<?> query) {
    return executeRequest(newPostMethod(query));
  }

  @Override
  public String execute(UpdateQuery<?> query) {
    return executeRequest(newPutMethod(query));
  }

  @Override
  public String execute(DeleteQuery query) {
    return executeRequest(newDeleteMethod(query));
  }

  private String executeRequest(HttpRequestBase request) {
    String json = null;
    try {
      BasicHttpContext context = createLocalContext(client);
      HttpResponse response = client.execute(request, context);
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          json = EntityUtils.toString(entity);

        } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND) {
          throw new ConnectionException("HTTP error: " + response.getStatusLine().getStatusCode()
            + ", msg: " + response.getStatusLine().getReasonPhrase()
            + ", query: " + request.toString());
        }
      }

    } catch (IOException e) {
      throw new ConnectionException("Query: " + request.getURI(), e);

    } finally {
      request.releaseConnection();
    }
    return json;
  }

  public void close() {
    if (client != null) {
      client.getConnectionManager().shutdown();
    }
  }

  private void initClient() {
    client = new DefaultHttpClient();
    HttpParams params = client.getParams();
    HttpConnectionParams.setConnectionTimeout(params, AbstractQuery.DEFAULT_TIMEOUT_MILLISECONDS);
    HttpConnectionParams.setSoTimeout(params, AbstractQuery.DEFAULT_TIMEOUT_MILLISECONDS);
    if (server.getUsername() != null) {
      client.getCredentialsProvider()
        .setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(server.getUsername(), server.getPassword()));
    }
  }

  private BasicHttpContext createLocalContext(DefaultHttpClient client) {
    BasicHttpContext localcontext = new BasicHttpContext();

    if (server.getUsername() != null) {
      // Generate BASIC scheme object and stick it to the local
      // execution context
      BasicScheme basicAuth = new BasicScheme();
      localcontext.setAttribute(PreemptiveAuth.ATTRIBUTE, basicAuth);

      // Add as the first request interceptor
      client.addRequestInterceptor(new PreemptiveAuth(), 0);
    }
    return localcontext;
  }

  private HttpGet newGetMethod(Query<?> query) {
    HttpGet get = new HttpGet(server.getHost() + query.getUrl());
    initRequest(get, query);
    return get;
  }

  private HttpDelete newDeleteMethod(DeleteQuery query) {
    HttpDelete delete = new HttpDelete(server.getHost() + query.getUrl());
    initRequest(delete, query);
    return delete;
  }

  private HttpPost newPostMethod(CreateQuery<?> query) {
    HttpPost post = new HttpPost(server.getHost() + query.getUrl());
    initRequest(post, query);
    setRequestEntity(post, query);
    return post;
  }

  private HttpPut newPutMethod(UpdateQuery<?> query) {
    HttpPut put = new HttpPut(server.getHost() + query.getUrl());
    initRequest(put, query);
    setRequestEntity(put, query);
    return put;
  }

  private void setRequestEntity(HttpEntityEnclosingRequestBase request, AbstractQuery<?> query) {
    if (query.getBody() != null) {
      try {
        request.setEntity(new StringEntity(query.getBody(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new ConnectionException("Encoding is not supported", e);
      }
    }
  }

  private void initRequest(HttpRequestBase request, AbstractQuery query) {
    request.setHeader("Accept", "application/json");
    if (query.getLocale() != null) {
      request.setHeader("Accept-Language", query.getLocale());
    }
    request.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, query.getTimeoutMilliseconds());
    request.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, query.getTimeoutMilliseconds());
  }

  static final class PreemptiveAuth implements HttpRequestInterceptor {

    static final String ATTRIBUTE = "preemptive-auth";

    @Override
    public void process(
      final HttpRequest request,
      final HttpContext context) throws HttpException {

      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

      // If no auth scheme available yet, try to initialize it preemptively
      if (authState.getAuthScheme() == null) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute(ATTRIBUTE);
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
