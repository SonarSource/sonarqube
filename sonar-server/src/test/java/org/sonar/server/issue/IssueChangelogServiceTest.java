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

import org.junit.Test;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.user.UserSession;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueChangelogServiceTest {

  IssueChangeDao changeDao = mock(IssueChangeDao.class);
  UserFinder userFinder = mock(UserFinder.class);
  IssueChangelogService service = new IssueChangelogService(changeDao, userFinder);

  @Test
  public void should_load_changelog_and_related_users() throws Exception {
    FieldDiffs userChange = new FieldDiffs().setUserLogin("arthur").setDiff("severity", "MAJOR", "BLOCKER");
    FieldDiffs scanChange = new FieldDiffs().setDiff("status", "RESOLVED", "CLOSED");
    when(changeDao.selectChangelogByIssue("ABCDE")).thenReturn(Arrays.asList(userChange, scanChange));
    User arthur = new DefaultUser().setLogin("arthur").setName("Arthur");
    when(userFinder.findByLogins(Arrays.asList("arthur"))).thenReturn(Arrays.asList(arthur));

    IssueChangelog changelog = service.changelog("ABCDE", mock(UserSession.class));

    assertThat(changelog).isNotNull();
    assertThat(changelog.changes()).containsOnly(userChange, scanChange);
    assertThat(changelog.user(scanChange)).isNull();
    assertThat(changelog.user(userChange)).isSameAs(arthur);
  }
}
