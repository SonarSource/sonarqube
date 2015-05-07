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

package org.sonar.server.issue;

import com.google.common.base.Strings;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.Map;

@ServerSide
public class CommentAction extends Action {

  public static final String KEY = "comment";
  public static final String COMMENT_PROPERTY = "comment";

  private final IssueUpdater issueUpdater;

  public CommentAction(IssueUpdater issueUpdater) {
    super(KEY);
    this.issueUpdater = issueUpdater;
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
    comment(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    issueUpdater.addComment((DefaultIssue) context.issue(), comment(properties), context.issueChangeContext());
    return true;
  }

  private String comment(Map<String, Object> properties) {
    String param = (String) properties.get(COMMENT_PROPERTY);
    if (Strings.isNullOrEmpty(param)) {
      throw new IllegalArgumentException("Missing parameter : '" + COMMENT_PROPERTY + "'");
    }
    return param;
  }
}
