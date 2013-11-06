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

package org.sonar.core.issue;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.core.issue.db.IssueChangeDao;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class IssueChangelogFinder implements BatchComponent {

  private final IssueChangeDao dao;

  public IssueChangelogFinder(IssueChangeDao dao) {
    this.dao = dao;
  }

  public Multimap<Issue, FieldDiffs> findByIssues(Collection<Issue> issues) {
    Multimap<Issue, FieldDiffs> changelogList = ArrayListMultimap.create();

    List<FieldDiffs> changelog = dao.selectChangelogByIssues(issueKey(issues));
    for (FieldDiffs fieldDiff : changelog) {
      changelogList.put(findIssueByKey(fieldDiff.issueKey(), issues), fieldDiff);
    }

    return changelogList;
  }

  private Collection<String> issueKey(Collection<Issue> issues) {
    return newArrayList(Iterables.transform(issues, new Function<Issue, String>() {
      @Override
      public String apply(Issue input) {
        return input.key();
      }
    }));
  }

  private Issue findIssueByKey(final String issueKey, Collection<Issue> issues){
    return Iterables.find(issues, new Predicate<Issue>() {
      @Override
      public boolean apply(@Nullable Issue input) {
        return issueKey.equals(input.key());
      }
    });
  }

}
