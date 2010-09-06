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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import org.sonar.api.web.gwt.client.Utils;

public final class ViolationsQuery extends AbstractResourceQuery<Violations> {

  private String scopes;
  private String qualifiers;
  private String rules;
  private String categories;
  private String priorities;
  private Integer depth;

  private ViolationsQuery(String resourceKey) {
    super(resourceKey);
  }

  public static ViolationsQuery create(String resourceKey) {
    return new ViolationsQuery(resourceKey);
  }

  public String getScopes() {
    return scopes;
  }

  public ViolationsQuery setScopes(String scopes) {
    this.scopes = scopes;
    return this;
  }

  public String getQualifiers() {
    return qualifiers;
  }

  public ViolationsQuery setQualifiers(String qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String getRules() {
    return rules;
  }

  public ViolationsQuery setRules(String rules) {
    this.rules = rules;
    return this;
  }

  public String getCategories() {
    return categories;
  }

  public ViolationsQuery setCategories(String s) {
    this.categories = s;
    return this;
  }

  public Integer getDepth() {
    return depth;
  }

  public ViolationsQuery setDepth(Integer depth) {
    this.depth = depth;
    return this;
  }

  public String getPriorities() {
    return priorities;
  }

  public ViolationsQuery setPriorities(String priorities) {
    this.priorities = priorities;
    return this;
  }

  @Override
  public String toString() {
    String url = Utils.getServerApiUrl() + "/violations?resource=" + getResourceKey() + "&";
    if (depth != null) {
      url += "depth=" + depth + "&";
    }
    if (scopes != null) {
      url += "scopes=" + scopes + "&";
    }
    if (qualifiers != null) {
      url += "qualifiers=" + qualifiers + "&";
    }
    if (rules != null) {
      url += "rules=" + rules + "&";
    }
    if (categories != null) {
      url += "categories=" + categories + "&";
    }
    if (priorities != null) {
      url += "priorities=" + priorities + "&";
    }
    return url;
  }

  @Override
  public void execute(final QueryCallBack<Violations> callback) {
    JsonUtils.requestJson(this.toString(), new JSONHandlerDispatcher<Violations>(callback) {
      @Override
      public Violations parseResponse(JavaScriptObject obj) {
        return parseJSON(obj);
      }
    });
  }

  private Violations parseJSON(JavaScriptObject obj) {
    Violations result = new Violations();
    JSONArray jsonArray = new JSONArray(obj);
    for (int i = 0; i < jsonArray.size(); i++) {
      JSONObject jsViolation = jsonArray.get(i).isObject();
      if (jsViolation == null) {
        continue;
      }
      JSONString message = jsViolation.get("message").isString();
      JSONString priority = jsViolation.get("priority").isString();
      JSONValue lineJson = jsViolation.get("line");
      int lineIndex = 0;
      if (lineJson != null) {
        lineIndex = (int) lineJson.isNumber().doubleValue();
      }

      JSONObject ruleObj = jsViolation.get("rule").isObject();
      Rule rule = new Rule(
          JsonUtils.getString(ruleObj, "key"),
          JsonUtils.getString(ruleObj, "name"),
          JsonUtils.getString(ruleObj, "category")
      );

      result.add(new Violation(message.stringValue(), priority.stringValue(), lineIndex, rule, null));
    }
    return result;
  }

}
