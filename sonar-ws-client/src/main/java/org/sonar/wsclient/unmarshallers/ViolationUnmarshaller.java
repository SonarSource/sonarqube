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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.WSUtils;

public class ViolationUnmarshaller extends AbstractUnmarshaller<Violation> {

  @Override
  protected Violation parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();

    Violation violation = new Violation();
    violation.setMessage(utils.getString(json, "message"));
    violation.setLine(utils.getInteger(json, "line"));
    violation.setSeverity(utils.getString(json, "priority"));
    violation.setCreatedAt(utils.getDateTime(json, "createdAt"));
    violation.setSwitchedOff(utils.getBoolean(json, "switchedOff"));

    Object rule = utils.getField(json, "rule");
    if (rule != null) {
      violation.setRuleKey(utils.getString(rule, "key"));
      violation.setRuleName(utils.getString(rule, "name"));
    }

    Object resource = utils.getField(json, "resource");
    if (resource != null) {
      violation.setResourceKey(utils.getString(resource, "key"));
      violation.setResourceName(utils.getString(resource, "name"));
      violation.setResourceQualifier(utils.getString(resource, "qualifier"));
    }
    return violation;
  }
}
