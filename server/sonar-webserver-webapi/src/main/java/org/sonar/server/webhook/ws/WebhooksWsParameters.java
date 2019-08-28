/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.webhook.ws;

class WebhooksWsParameters {

  static final String WEBHOOKS_CONTROLLER = "api/webhooks";


  static final String LIST_ACTION = "list";
  static final String ACTION_CREATE = "create";
  static final String UPDATE_ACTION = "update";
  static final String DELETE_ACTION = "delete";


  static final String ORGANIZATION_KEY_PARAM = "organization";
  static final int ORGANIZATION_KEY_PARAM_MAXIMUM_LENGTH = 255;
  static final String PROJECT_KEY_PARAM = "project";
  static final int PROJECT_KEY_PARAM_MAXIMUM_LENGTH = 400;
  static final String NAME_PARAM = "name";
  static final int NAME_PARAM_MAXIMUM_LENGTH = 100;
  static final String URL_PARAM = "url";
  static final int URL_PARAM_MAXIMUM_LENGTH = 512;
  static final String KEY_PARAM = "webhook";
  static final int KEY_PARAM_MAXIMUM_LENGTH = 40;
  static final String SECRET_PARAM = "secret";
  static final int SECRET_PARAM_MAXIMUM_LENGTH = 200;

  private WebhooksWsParameters() {
    // prevent instantiation
  }

}
