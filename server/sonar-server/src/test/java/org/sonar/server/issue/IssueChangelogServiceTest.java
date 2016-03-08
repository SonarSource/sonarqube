/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.user.DefaultUser;
import org.sonar.db.issue.IssueChangeDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangelogServiceTest {

  @Mock
  IssueChangeDao changeDao;

  @Mock
  UserFinder userFinder;

  @Mock
  IssueService issueService;

  IssueChangelogService service;

  @Before
  public void setUp() {
    service = new IssueChangelogService(changeDao, userFinder, issueService);
  }

  @Test
  public void load_changelog_and_related_users() {
    FieldDiffs userChange = new FieldDiffs().setUserLogin("arthur").setDiff("severity", "MAJOR", "BLOCKER");
    FieldDiffs scanChange = new FieldDiffs().setDiff("status", "RESOLVED", "CLOSED");
    when(changeDao.selectChangelogByIssue("ABCDE")).thenReturn(Arrays.asList(userChange, scanChange));
    User arthur = new DefaultUser().setLogin("arthur").setName("Arthur");
    when(userFinder.findByLogins(Arrays.asList("arthur"))).thenReturn(Arrays.asList(arthur));

    when(issueService.getByKey("ABCDE")).thenReturn(new DefaultIssue().setKey("ABCDE"));

    IssueChangelog changelog = service.changelog("ABCDE");

    assertThat(changelog).isNotNull();
    assertThat(changelog.changes()).containsOnly(userChange, scanChange);
    assertThat(changelog.user(scanChange)).isNull();
    assertThat(changelog.user(userChange)).isSameAs(arthur);
  }

  @Test
  public void rename_technical_debt_change_to_effort() {
    FieldDiffs userChange = new FieldDiffs().setUserLogin("arthur").setDiff("technicalDebt", "10min", "30min");
    when(changeDao.selectChangelogByIssue("ABCDE")).thenReturn(Arrays.asList(userChange));
    User arthur = new DefaultUser().setLogin("arthur").setName("Arthur");
    when(userFinder.findByLogins(Arrays.asList("arthur"))).thenReturn(Arrays.asList(arthur));
    when(issueService.getByKey("ABCDE")).thenReturn(new DefaultIssue().setKey("ABCDE"));

    IssueChangelog changelog = service.changelog("ABCDE");

    assertThat(changelog).isNotNull();
    assertThat(changelog.changes()).hasSize(1);
    assertThat(changelog.changes().get(0).diffs()).containsKeys("effort");
  }

}
