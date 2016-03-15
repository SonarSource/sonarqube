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
package org.sonar.ce.es;

import org.junit.Test;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.view.index.ViewIndexer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EsIndexerEnablerTest {
  private TestIndexer testIndexer = mock(TestIndexer.class);
  private IssueAuthorizationIndexer issueAuthorizationIndexer = mock(IssueAuthorizationIndexer.class);
  private IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private UserIndexer userIndexer = mock(UserIndexer.class);
  private ViewIndexer viewIndexer = mock(ViewIndexer.class);
  private ActivityIndexer activityIndexer = mock(ActivityIndexer.class);
  private EsIndexerEnabler underTest = new EsIndexerEnabler(testIndexer, issueAuthorizationIndexer, issueIndexer, userIndexer, viewIndexer, activityIndexer);

  @Test
  public void start_enables_all_indexers() {
    underTest.start();

    verify(testIndexer).setEnabled(true);
    verify(issueAuthorizationIndexer).setEnabled(true);
    verify(issueIndexer).setEnabled(true);
    verify(userIndexer).setEnabled(true);
    verify(viewIndexer).setEnabled(true);
    verify(activityIndexer).setEnabled(true);
  }
}
