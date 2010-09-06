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

import org.sonar.api.web.gwt.client.Utils;
import org.sonar.api.web.gwt.client.widgets.LoadingLabel;

public abstract class BaseQueryCallback<P extends ResponsePOJO> implements QueryCallBack<P> {

  private LoadingLabel loading;

  public BaseQueryCallback() {
    this(null);
  }

  public BaseQueryCallback(LoadingLabel loading) {
    super();
    this.loading = loading;
  }

  public void onError(int errorCode, String errorMessage) {
    Utils.showError("Error received from server : " + errorCode + " - " + errorMessage);
    if (loading != null) {
      loading.removeFromParent();
    }
  }

  public void onTimeout() {
    Utils.showWarning("JSON query response timeout");
    if (loading != null) {
      loading.removeFromParent();
    }
  }

}