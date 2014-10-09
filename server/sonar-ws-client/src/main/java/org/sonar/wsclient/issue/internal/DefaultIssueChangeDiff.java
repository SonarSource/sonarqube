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
package org.sonar.wsclient.issue.internal;

import org.sonar.wsclient.issue.IssueChangeDiff;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.CheckForNull;

import java.util.Map;

/**
 * @since 4.1
 */
public class DefaultIssueChangeDiff implements IssueChangeDiff {

  private final Map json;

  DefaultIssueChangeDiff(Map json) {
    this.json = json;
  }

  @Override
  public String key() {
    return JsonUtils.getString(json, "key");
  }

  @Override
  @CheckForNull
  public Object newValue() {
    return parseValue("newValue");

  }

  @Override
  @CheckForNull
  public Object oldValue() {
    return parseValue("oldValue");
  }

  private Object parseValue(String attribute) {
    return JsonUtils.getString(json, attribute);
  }

}
