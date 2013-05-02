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

import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class IssuePersisterTest extends AbstractDaoTestCase {

  IssuePersister persister;

  @Test
  public void should_persist_all_issues() throws Exception {
    List<DefaultIssue> issues = Arrays.asList(new DefaultIssue());
    IssueCache issueCache = mock(IssueCache.class);
    when(issueCache.componentIssues()).thenReturn(issues);
    ScanIssueStorage storage = mock(ScanIssueStorage.class);

    persister = new IssuePersister(issueCache, storage);
    persister.persist();

    verify(storage, times(1)).save(issues);
  }
}
