/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.db.IssueStatsColumn;
import org.sonar.core.issue.db.IssueStatsDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 3.6
 */
public class IssueStatsFinder implements ServerComponent {

  private final IssueStatsDao issuestatsDao;
  private final UserFinder userFinder;

  public IssueStatsFinder(IssueStatsDao issuestatsDao, UserFinder userFinder) {
    this.issuestatsDao = issuestatsDao;
    this.userFinder = userFinder;
  }

  public IssueStatsResult findIssueAssignees(IssueQuery query) {
    List<Object> results = issuestatsDao.selectIssuesColumn(query, IssueStatsColumn.ASSIGNEE, UserSession.get().userId());

    Set<String> users = Sets.newHashSet();
    for (Object result : results) {
      if (result != null) {
        users.add((String) result);
      }
    }

    return new IssueStatsResult(results).addUsers(findUsers(users));
  }

  private Collection<User> findUsers(Set<String> logins) {
    return userFinder.findByLogins(Lists.newArrayList(logins));
  }

  public static class IssueStatsResult {
    private final Map<String, User> usersByLogin = Maps.newHashMap();
    private List<Object> results;

    public IssueStatsResult(List<Object> results) {
      this.results = results;
    }

    public IssueStatsResult addUsers(Collection<User> users) {
      for (User user : users) {
        usersByLogin.put(user.login(), user);
      }
      return this;
    }

    public List<Object> results() {
      return results;
    }

    @CheckForNull
    public User user(String login) {
      return usersByLogin.get(login);
    }
  }
}
