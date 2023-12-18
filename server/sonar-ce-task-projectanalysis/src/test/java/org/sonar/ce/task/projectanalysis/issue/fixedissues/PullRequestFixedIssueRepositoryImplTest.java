/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue.fixedissues;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;

public class PullRequestFixedIssueRepositoryImplTest {

  @Test
  public void getFixedIssues_shouldReturnListOfIssues() {
    PullRequestFixedIssueRepositoryImpl fixedIssueRepository = new PullRequestFixedIssueRepositoryImpl();
    fixedIssueRepository.addFixedIssue(new DefaultIssue().setKey("key1"));
    fixedIssueRepository.addFixedIssue(new DefaultIssue().setKey("key2"));

    Assertions.assertThat(fixedIssueRepository.getFixedIssues()).extracting(DefaultIssue::key)
      .containsExactly("key1", "key2");
  }

}
