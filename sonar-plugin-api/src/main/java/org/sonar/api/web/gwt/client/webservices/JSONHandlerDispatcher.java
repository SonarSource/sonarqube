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
package org.sonar.api.web.gwt.client.webservices;

import com.google.gwt.core.client.JavaScriptObject;

public abstract class JSONHandlerDispatcher<P extends ResponsePOJO> implements JsonUtils.JSONHandler {

  private QueryCallBack<P> callBack;

  public JSONHandlerDispatcher(QueryCallBack<P> callBack) {
    super();
    this.callBack = callBack;
  }

  public abstract P parseResponse(JavaScriptObject obj);

  public void onError(int errorCode, String errorMessage) {
    callBack.onError(errorCode, errorMessage);
  }

  public void onResponse(JavaScriptObject obj) {
    P responseObj = parseResponse(obj);
    callBack.onResponse(responseObj, obj);
  }

  public void onTimeout() {
    callBack.onTimeout();
  }

}
