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
package org.sonar.wsclient.qualitygate.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.qualitygate.*;

import java.util.*;

public class DefaultQualityGateClient implements QualityGateClient {

  private static final String ROOT_URL = "/api/qualitygates";
  private static final String LIST_URL = ROOT_URL + "/list";
  private static final String SHOW_URL = ROOT_URL + "/show";
  private static final String CREATE_URL = ROOT_URL + "/create";
  private static final String CREATE_CONDITION_URL = ROOT_URL + "/create_condition";
  private static final String UPDATE_CONDITION_URL = ROOT_URL + "/update_condition";
  private static final String DELETE_CONDITION_URL = ROOT_URL + "/delete_condition";
  private static final String RENAME_URL = ROOT_URL + "/rename";
  private static final String DESTROY_URL = ROOT_URL + "/destroy";
  private static final String SET_DEFAULT_URL = ROOT_URL + "/set_as_default";
  private static final String UNSET_DEFAULT_URL = ROOT_URL + "/unset_default";
  private static final String SELECT_URL = ROOT_URL + "/select";
  private static final String DESELECT_URL = ROOT_URL + "/deselect";

  private final HttpRequestFactory requestFactory;

  public DefaultQualityGateClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public QualityGates list() {
    String json = requestFactory.get(LIST_URL, Collections.<String, Object> emptyMap());
    return jsonToQualityGates(json);
  }

  @Override
  public QualityGate create(String qGateName) {
    String json = requestFactory.post(CREATE_URL, Collections.singletonMap("name", (Object) qGateName));
    return jsonToQualityGate(json);
  }

  @Override
  public QualityGate rename(long qGateId, String qGateName) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("id", qGateId);
    params.put("name", qGateName);
    String json = requestFactory.post(RENAME_URL, params);
    return jsonToQualityGate(json);
  }

  @Override
  public QualityGateDetails show(long qGateId) {
    String json = requestFactory.get(SHOW_URL, Collections.singletonMap("id", (Object) qGateId));
    return jsonToDetails(json);
  }

  @Override
  public QualityGateDetails show(String qGateName) {
    String json = requestFactory.get(SHOW_URL, Collections.singletonMap("name", (Object) qGateName));
    return jsonToDetails(json);
  }

  @Override
  public QualityGateCondition createCondition(NewCondition condition) {
    String json = requestFactory.post(CREATE_CONDITION_URL, condition.urlParams());
    return jsonToCondition(json);
  }

  @Override
  public QualityGateCondition updateCondition(UpdateCondition condition) {
    String json = requestFactory.post(UPDATE_CONDITION_URL, condition.urlParams());
    return jsonToCondition(json);
  }

  @Override
  public void deleteCondition(long conditionId) {
    requestFactory.post(DELETE_CONDITION_URL, Collections.singletonMap("id", (Object) conditionId));
  }

  @Override
  public void destroy(long qGateId) {
    requestFactory.post(DESTROY_URL, Collections.singletonMap("id", (Object) qGateId));
  }

  @Override
  public void setDefault(long qGateId) {
    requestFactory.post(SET_DEFAULT_URL, Collections.singletonMap("id", (Object) qGateId));
  }

  @Override
  public void unsetDefault() {
    requestFactory.post(UNSET_DEFAULT_URL, Collections.<String, Object> emptyMap());
  }

  @Override
  public void selectProject(long qGateId, long projectId) {
    requestFactory.post(SELECT_URL, selectionParams(qGateId, projectId));
  }

  @Override
  public void deselectProject(long qGateId, long projectId) {
    requestFactory.post(DESELECT_URL, selectionParams(qGateId, projectId));
  }

  private Map<String, Object> selectionParams(long qGateId, long projectId) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("gateId", Long.toString(qGateId));
    params.put("projectId", Long.toString(projectId));
    return params;
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  private QualityGate jsonToQualityGate(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultQualityGate((Map) jsonRoot);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private QualityGates jsonToQualityGates(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultQualityGates((Map) jsonRoot);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private QualityGateDetails jsonToDetails(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultQualityGateDetails((Map) jsonRoot, jsonToConditions(json));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Collection<QualityGateCondition> jsonToConditions(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    Collection<Map> conditionArray = (Collection<Map>) jsonRoot.get("conditions");
    Collection<QualityGateCondition> conditions = new ArrayList<QualityGateCondition>();
    if (conditionArray != null) {
      for (Map conditionJson: conditionArray) {
        conditions.add(new DefaultQualityGateCondition(conditionJson));
      }
    }
    return conditions;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private DefaultQualityGateCondition jsonToCondition(String json) {
    return new DefaultQualityGateCondition((Map) JSONValue.parse(json));
  }
}
