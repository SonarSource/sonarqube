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
package org.sonar.batch.issue.tracking;

import org.sonar.batch.issue.tracking.IssueHandlers;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.issue.IssueHandler;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;

import java.util.Date;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class IssueHandlersTest {
  @Test
  public void should_execute_handlers() throws Exception {
    IssueHandler h1 = mock(IssueHandler.class);
    IssueHandler h2 = mock(IssueHandler.class);
    IssueUpdater updater = mock(IssueUpdater.class);

    IssueHandlers handlers = new IssueHandlers(updater, new IssueHandler[]{h1, h2});
    final DefaultIssue issue = new DefaultIssue();
    handlers.execute(issue, IssueChangeContext.createScan(new Date()));

    verify(h1).onIssue(argThat(new ArgumentMatcher<IssueHandler.Context>() {
      @Override
      public boolean matches(Object o) {
        return ((IssueHandler.Context) o).issue() == issue;
      }
    }));
  }

  @Test
  public void test_no_handlers() {
    IssueUpdater updater = mock(IssueUpdater.class);
    IssueHandlers handlers = new IssueHandlers(updater);
    handlers.execute(new DefaultIssue(), IssueChangeContext.createScan(new Date()));
    verifyZeroInteractions(updater);
  }
}
