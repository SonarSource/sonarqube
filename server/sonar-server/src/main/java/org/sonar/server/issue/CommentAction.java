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
package org.sonar.server.issue;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.user.UserSession;

@ServerSide
public class CommentAction extends Action {

  public static final String COMMENT_KEY = "comment";
  public static final String COMMENT_PROPERTY = "comment";

  private final IssueFieldsSetter issueUpdater;

  public CommentAction(IssueFieldsSetter issueUpdater) {
    super(COMMENT_KEY);
    this.issueUpdater = issueUpdater;
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    comment(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    issueUpdater.addComment(context.issue(), comment(properties), context.issueChangeContext());
    return true;
  }

  @Override
  public boolean shouldRefreshMeasures() {
    return false;
  }

  private static String comment(Map<String, Object> properties) {
    String param = (String) properties.get(COMMENT_PROPERTY);
    if (Strings.isNullOrEmpty(param)) {
      throw new IllegalArgumentException("Missing parameter : '" + COMMENT_PROPERTY + "'");
    }
    return param;
  }
}
