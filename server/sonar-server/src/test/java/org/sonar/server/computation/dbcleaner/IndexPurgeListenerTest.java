/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.dbcleaner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.test.index.TestIndexer;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IndexPurgeListenerTest {

  TestIndexer testIndexer = mock(TestIndexer.class);
  IssueIndexer issueIndexer = mock(IssueIndexer.class);
  ComponentIndexer componentIndexer = mock(ComponentIndexer.class);

  IndexPurgeListener underTest = new IndexPurgeListener(testIndexer, issueIndexer, componentIndexer);

  @Test
  public void test_onComponentDisabling() {
    String uuid = "123456";
    String projectUuid = "P789";
    List<String> uuids = Arrays.asList(uuid);
    underTest.onComponentsDisabling(projectUuid, uuids);

    verify(testIndexer).deleteByFile(uuid);
    verify(componentIndexer).delete(projectUuid, uuids);
  }

  @Test
  public void test_onIssuesRemoval() {
    underTest.onIssuesRemoval("P1", asList("ISSUE1", "ISSUE2"));

    verify(issueIndexer).deleteByKeys("P1", asList("ISSUE1", "ISSUE2"));
  }

}
