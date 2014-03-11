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
package org.sonar.wsclient.issue;

import org.sonar.wsclient.internal.EncodingUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.7
 */
public class BulkChangeQuery {

  private final Map<String, Object> params = new HashMap<String, Object>();

  private BulkChangeQuery() {
  }

  public static BulkChangeQuery create() {
    return new BulkChangeQuery();
  }

  /**
   * URL query string, for internal use
   */
  public Map<String, Object> urlParams() {
    return params;
  }

  public BulkChangeQuery issues(String... keys) {
    return addParam("issues", keys);
  }

  public BulkChangeQuery actions(String... actions) {
    return addParam("actions", actions);
  }

  public BulkChangeQuery actionParameter(String action, String parameter, Object value) {
    params.put(action + "." + parameter, value);
    return this;
  }

  public BulkChangeQuery comment(String comment) {
    params.put("comment", comment);
    return this;
  }

  public BulkChangeQuery sendNotifications(boolean sendNotifications) {
    params.put("sendNotifications", String.valueOf(sendNotifications));
    return this;
  }

  private BulkChangeQuery addParam(String key, String[] values) {
    if (values != null) {
      params.put(key, EncodingUtils.toQueryParam(values));
    }
    return this;
  }

}
