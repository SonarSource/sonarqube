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
package org.sonar.server.issue.index;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.search.BaseDoc;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class IssueDoc extends BaseDoc implements Issue {

  public IssueDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public String key() {
    return getField(IssueNormalizer.IssueField.KEY.field());
  }

  @Override
  public String componentKey() {
    return null;
  }

  @Override
  public RuleKey ruleKey() {
    return null;
  }

  @Override
  public String severity() {
    return null;
  }

  @Override
  public String message() {
    return null;
  }

  @Override
  public Integer line() {
    return null;
  }

  @Override
  public Double effortToFix() {
    return null;
  }

  @Override
  public String status() {
    return null;
  }

  @Override
  public String resolution() {
    return null;
  }

  @Override
  public String reporter() {
    return null;
  }

  @Override
  public String assignee() {
    return null;
  }

  @Override
  public Date creationDate() {
    return null;
  }

  @Override
  public Date updateDate() {
    return null;
  }

  @Override
  public Date closeDate() {
    return null;
  }

  @Override
  public String attribute(String key) {
    return null;
  }

  @Override
  public Map<String, String> attributes() {
    return null;
  }

  @Override
  public String authorLogin() {
    return null;
  }

  @Override
  public String actionPlanKey() {
    return null;
  }

  @Override
  public List<IssueComment> comments() {
    return null;
  }

  @Override
  public boolean isNew() {
    return false;
  }
}
