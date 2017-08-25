/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.ws.client.setting;

import org.sonarqube.ws.Settings.ListDefinitionsWsResponse;
import org.sonarqube.ws.Settings.ValuesWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_LIST_DEFINITIONS;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_RESET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_SET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_VALUES;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.CONTROLLER_SETTINGS;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_FIELD_VALUES;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEYS;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_VALUE;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_VALUES;

public class SettingsService extends BaseService {
  public SettingsService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_SETTINGS);
  }

  public ListDefinitionsWsResponse listDefinitions(ListDefinitionsRequest request) {
    GetRequest getRequest = new GetRequest(path(ACTION_LIST_DEFINITIONS))
      .setParam(PARAM_COMPONENT, request.getComponent());
    return call(getRequest, ListDefinitionsWsResponse.parser());
  }

  public ValuesWsResponse values(ValuesRequest request) {
    GetRequest getRequest = new GetRequest(path(ACTION_VALUES))
      .setParam(PARAM_KEYS, inlineMultipleParamValue(request.getKeys()))
      .setParam(PARAM_COMPONENT, request.getComponent());
    return call(getRequest, ValuesWsResponse.parser());
  }

  public void set(SetRequest request) {
    call(new PostRequest(path(ACTION_SET))
      .setParam(PARAM_KEY, request.getKey())
      .setParam(PARAM_VALUE, request.getValue())
      .setParam(PARAM_VALUES, request.getValues())
      .setParam(PARAM_FIELD_VALUES, request.getFieldValues())
      .setParam(PARAM_COMPONENT, request.getComponent())
      .setParam(PARAM_BRANCH, request.getBranch())
    );
  }

  public void reset(ResetRequest request) {
    call(new PostRequest(path(ACTION_RESET))
      .setParam(PARAM_KEYS, inlineMultipleParamValue(request.getKeys()))
      .setParam(PARAM_COMPONENT, request.getComponent()));
  }

}
