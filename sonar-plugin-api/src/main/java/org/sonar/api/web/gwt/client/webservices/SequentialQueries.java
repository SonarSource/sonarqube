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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;

public class SequentialQueries extends Query<VoidResponse> {
  
  private List<AjaxQuery<?>> queries = new ArrayList<AjaxQuery<?>>();
  private int sleepTimeBetweenCbChecks;

  private SequentialQueries(int sleepTimeBetweenCbChecks) {
    this.sleepTimeBetweenCbChecks = sleepTimeBetweenCbChecks;
  }

  public static SequentialQueries get() {
    return new SequentialQueries(50);
  }

  public static SequentialQueries get(int sleepTimeBetweenCbChecks) {
    return new SequentialQueries(sleepTimeBetweenCbChecks);
  }
  
  public <R extends ResponsePOJO> SequentialQueries add(Query<R> query, QueryCallBack<R> callback) {
    queries.add(new AjaxQuery<R>(query, callback));
    return this;
  }

  @Override
  public void execute(final QueryCallBack<VoidResponse> callback) {
    for (AjaxQuery<?> query : queries) {
      query.execute();
    }
    Timer queriesMonitor = new Timer() {
      @Override
      public void run() {
        boolean queriesExecuted = true;
        for (AjaxQuery<?> query : queries) {
          if (!query.isCompleted()) {
            queriesExecuted = false;
            break;
          }
        }
        if (queriesExecuted) {
          callback.onResponse(new VoidResponse(), null);
          cancel();
        }
      }
    };
    queriesMonitor.scheduleRepeating(sleepTimeBetweenCbChecks);
  }
  
  private class AjaxQuery<R extends ResponsePOJO> {
    private Query<R> query;
    private QueryCallBack<R> callback;
    
    private boolean completed = false;

    public AjaxQuery(Query<R> query, QueryCallBack<R> callback) {
      super();
      this.query = query;
      this.callback = callback;
    }

    private void execute() {
      QueryCallBack<R> proxy = new QueryCallBack<R>() {
        public void onError(int errorCode, String errorMessage) {
          callback.onError(errorCode, errorMessage);
          completed = true;
        }

        public void onResponse(R response, JavaScriptObject jsonRawResponse) {
          callback.onResponse(response, jsonRawResponse);
          completed = true;
        }

        public void onTimeout() {
          callback.onTimeout();
          completed = true;
        }
      };
      query.execute(proxy);
    }

    public boolean isCompleted() {
      return completed;
    }
    
  }

}
