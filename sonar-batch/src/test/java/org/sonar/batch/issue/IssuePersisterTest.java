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
package org.sonar.batch.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssuePersisterTest extends AbstractDaoTestCase {

  IssuePersister persister;
  private ScanIssueStorage storage;
  private List<DefaultIssue> issues;
  private Settings settings;

  @Before
  public void prepare() {
    issues = Arrays.asList(new DefaultIssue());
    IssueCache issueCache = mock(IssueCache.class);
    when(issueCache.all()).thenReturn(issues);
    storage = mock(ScanIssueStorage.class);

    settings = new Settings();
    persister = new IssuePersister(issueCache, storage, settings);
  }

  @Test
  public void should_persist_all_issues() throws Exception {
    persister.persist();

    verify(storage, times(1)).save(issues);
  }

  @Test
  public void should_not_persist_issues_in_dry_run() throws Exception {
    settings.setProperty("sonar.dryRun", true);

    persister.persist();

    verify(storage, never()).save(issues);
  }
}
