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

package org.sonar.plugins.core.issue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import org.sonar.api.BatchExtension;
import org.sonar.core.issue.db.IssueDto;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class InitialOpenIssuesStack implements BatchExtension {

  private final ListMultimap<Integer, IssueDto> issuesByResourceId;

  private Date loadedDate;

  public InitialOpenIssuesStack() {
    issuesByResourceId = ArrayListMultimap.create();
  }

  public void setIssues(List<IssueDto> issues, Date loadedDate) {
    this.loadedDate = loadedDate;
    for (IssueDto issueDto : issues) {
      issuesByResourceId.put(issueDto.getComponentId(), issueDto);
    }
  }

  public List<IssueDto> selectAndRemove(final Integer resourceId) {
    List<IssueDto> foundIssuesDto = issuesByResourceId.get(resourceId);
    if (!foundIssuesDto.isEmpty()) {
      List<IssueDto> issuesDto = ImmutableList.copyOf(foundIssuesDto);
      issuesByResourceId.removeAll(resourceId);
      return issuesDto;
    } else {
      return Collections.emptyList();
    }
  }

  public Collection<IssueDto> getAllIssues() {
    return issuesByResourceId.values();
  }

  public Date getLoadedDate() {
    return loadedDate;
  }
}
