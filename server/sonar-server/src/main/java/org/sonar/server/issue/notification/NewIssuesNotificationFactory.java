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

package org.sonar.server.issue.notification;

import org.sonar.api.ServerSide;
import org.sonar.api.utils.Durations;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.user.index.UserIndex;

@ServerSide
public class NewIssuesNotificationFactory {
  private final UserIndex userIndex;
  private final RuleIndex ruleIndex;
  private final DbClient dbClient;
  private final Durations durations;

  public NewIssuesNotificationFactory(UserIndex userIndex, RuleIndex ruleIndex, DbClient dbClient, Durations durations) {
    this.userIndex = userIndex;
    this.ruleIndex = ruleIndex;
    this.dbClient = dbClient;
    this.durations = durations;
  }

  public MyNewIssuesNotification newMyNewIssuesNotification() {
    return new MyNewIssuesNotification(userIndex, ruleIndex, dbClient, durations);
  }

  public NewIssuesNotification newNewIssuesNotication() {
    return new NewIssuesNotification(userIndex, ruleIndex, dbClient, durations);
  }
}
