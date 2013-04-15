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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Issue;
import org.sonar.wsclient.services.WSUtils;

public class IssueUnmarshaller extends AbstractUnmarshaller<Issue> {

  @Override
  protected Issue parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    return new Issue()
        .setKey(utils.getString(json, "key"))
        .setComponentKey(utils.getString(json, "component"))
        .setRuleKey(utils.getString(json, "rule"))
        .setRuleRepositoryKey(utils.getString(json, "ruleRepository"))
        .setSeverity(utils.getString(json, "severity"))
        .setTitle(utils.getString(json, "title"))
        .setMessage(utils.getString(json, "message"))
        .setLine(utils.getInteger(json, "line"))
        .setCost(utils.getDouble(json, "cost"))
        .setStatus(utils.getString(json, "status"))
        .setResolution(utils.getString(json, "resolution"))
        .setUserLogin(utils.getString(json, "userLogin"))
        .setAssigneeLogin(utils.getString(json, "assigneeLogin"))
        .setCreatedAt(utils.getDateTime(json, "createdAt"))
        .setUpdatedAt(utils.getDateTime(json, "updatedAt"))
        .setClosedAt(utils.getDateTime(json, "closedAt"));
  }
}
