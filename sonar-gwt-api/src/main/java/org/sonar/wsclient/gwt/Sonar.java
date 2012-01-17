/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.wsclient.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONObject;
import org.sonar.gwt.JsonUtils;
import org.sonar.wsclient.services.*;
import org.sonar.wsclient.unmarshallers.Unmarshaller;
import org.sonar.wsclient.unmarshallers.Unmarshallers;

public class Sonar {
  static {
    WSUtils.setInstance(new GwtUtils());
  }

  private static Sonar instance = null;

  private final String host;

  public Sonar(String host) {
    this.host = host;
  }

  /**
   * To be used in Sonar extensions only, else use constructors.
   */
  public static Sonar getInstance() {
    if (instance == null) {
      Dictionary dic = Dictionary.getDictionary("config");
      instance = new Sonar(dic.get("sonar_url"));
    }
    return instance;
  }

  public <MODEL extends Model> void find(final Query<MODEL> query, final Callback<MODEL> callback) {
    JsonUtils.requestJson(getUrl(query), new JsonUtils.JSONHandler() {
      public void onResponse(JavaScriptObject obj) {
        Unmarshaller<MODEL> unmarshaller = Unmarshallers.forModel(query.getModelClass());
        String json = (new JSONObject(obj)).toString();
        callback.onResponse(unmarshaller.toModel(json), obj);
      }

      public void onTimeout() {
        callback.onTimeout();
      }

      public void onError(int errorCode, String errorMessage) {
        callback.onError(errorCode, errorMessage);
      }
    });
  }

  public <MODEL extends Model> void findAll(final Query<MODEL> query, final ListCallback<MODEL> callback) {
    JsonUtils.requestJson(getUrl(query), new JsonUtils.JSONHandler() {
      public void onResponse(JavaScriptObject obj) {
        Unmarshaller<MODEL> unmarshaller = Unmarshallers.forModel(query.getModelClass());
        String json = (new JSONObject(obj)).toString();
        callback.onResponse(unmarshaller.toModels(json), obj);
      }

      public void onTimeout() {
        callback.onTimeout();
      }

      public void onError(int errorCode, String errorMessage) {
        callback.onError(errorCode, errorMessage);
      }
    });
  }

  public void delete(final DeleteQuery query, final SimpleCallback callback) {
    createUpdate(query, callback, RequestBuilder.DELETE);
  }

  public <MODEL extends Model> void create(final CreateQuery<MODEL> query, final SimpleCallback callback) {
    createUpdate(query, callback, RequestBuilder.POST);
  }

  public <MODEL extends Model> void update(final UpdateQuery<MODEL> query, final SimpleCallback callback) {
    createUpdate(query, callback, RequestBuilder.PUT);
  }

  private void createUpdate(final AbstractQuery query, final SimpleCallback callback, RequestBuilder.Method method) {
    JsonUtils.request(getUrl(query), new JsonUtils.SimpleHandler() {
      public void onSuccess() {
        callback.onSuccess();
      }

      public void onTimeout() {
        callback.onTimeout();
      }

      public void onError(int errorCode, String errorMessage) {
        callback.onError(errorCode, errorMessage);
      }
    }, method, query.getBody());
  }

  private String getUrl(AbstractQuery query) {
    return host + query.getUrl();
  }
}
