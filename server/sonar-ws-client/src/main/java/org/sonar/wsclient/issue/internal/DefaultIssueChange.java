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

import org.sonar.wsclient.issue.IssueChange;
import org.sonar.wsclient.issue.IssueChangeDiff;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 4.1
 */
public class DefaultIssueChange implements IssueChange {

  private final Map json;

  DefaultIssueChange(Map json) {
    this.json = json;
  }

  @Override
  public String user() {
    return JsonUtils.getString(json, "user");
  }

  @Override
  public Date creationDate() {
    return JsonUtils.getDateTime(json, "creationDate");
  }

  @Override
  public List<IssueChangeDiff> diffs() {
    List<IssueChangeDiff> diffs = new ArrayList<IssueChangeDiff>();
    List<Map> jsonDiffs = (List<Map>) json.get("diffs");
    for (Map jsonDiff : jsonDiffs) {
      diffs.add(new DefaultIssueChangeDiff(jsonDiff));
    }
    return diffs;
  }

}
