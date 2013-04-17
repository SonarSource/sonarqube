/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.batch.issue;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.BatchExtension;
import org.sonar.core.issue.IssueDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class InitialOpenIssuesStack implements BatchExtension {

  private List<IssueDto> issues;

  public InitialOpenIssuesStack() {

  }

  public void setIssues(List<IssueDto> issues){
    this.issues = issues;
  }

  public List<IssueDto> selectAndRemove(final Integer resourceId){
    Predicate resourcePredicate = new Predicate<IssueDto>() {
      @Override
      public boolean apply(IssueDto issueDto) {
        return issueDto.getResourceId().equals(resourceId);
      }
    };

    List<IssueDto> issuesDto = newArrayList(Iterables.find(issues, resourcePredicate));
    Iterables.removeIf(issuesDto, resourcePredicate);

    return issuesDto;
  }
}
