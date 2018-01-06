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
package org.sonar.server.issue;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.rule.Severity;
import org.sonar.db.issue.IssueDto;

/**
 * @since 3.6
 */
class IssuesFinderSort {

  private List<IssueDto> issues;
  private IssueQuery query;

  public IssuesFinderSort(List<IssueDto> issues, IssueQuery query) {
    this.issues = issues;
    this.query = query;
  }

  public List<IssueDto> sort() {
    String sort = query.sort();
    Boolean asc = query.asc();
    if (sort != null && asc != null) {
      return getIssueProcessor(sort).sort(issues, asc);
    }
    return issues;
  }

  private static IssueProcessor getIssueProcessor(String sort) {
    if (IssueQuery.SORT_BY_ASSIGNEE.equals(sort)) {
      return new AssigneeSortIssueProcessor();
    }
    if (IssueQuery.SORT_BY_SEVERITY.equals(sort)) {
      return new SeveritySortIssueProcessor();
    }
    if (IssueQuery.SORT_BY_STATUS.equals(sort)) {
      return new StatusSortIssueProcessor();
    }
    if (IssueQuery.SORT_BY_CREATION_DATE.equals(sort)) {
      return new CreationDateSortIssueProcessor();
    }
    if (IssueQuery.SORT_BY_UPDATE_DATE.equals(sort)) {
      return new UpdateDateSortIssueProcessor();
    }
    if (IssueQuery.SORT_BY_CLOSE_DATE.equals(sort)) {
      return new CloseDateSortIssueProcessor();
    }
    throw new IllegalArgumentException("Cannot sort on field : " + sort);
  }

  interface IssueProcessor {
    Function sortFieldFunction();

    Ordering sortFieldOrdering(boolean ascending);

    default List<IssueDto> sort(Collection<IssueDto> issueDtos, boolean ascending) {
      Ordering<IssueDto> ordering = sortFieldOrdering(ascending).onResultOf(sortFieldFunction());
      return ordering.immutableSortedCopy(issueDtos);
    }
  }

  abstract static class TextSortIssueProcessor implements IssueProcessor {
    @Override
    public Function sortFieldFunction() {
      return new Function<IssueDto, String>() {
        @Override
        public String apply(IssueDto issueDto) {
          return sortField(issueDto);
        }
      };
    }

    abstract String sortField(IssueDto issueDto);

    @Override
    public Ordering sortFieldOrdering(boolean ascending) {
      Ordering<String> ordering = Ordering.from(String.CASE_INSENSITIVE_ORDER).nullsLast();
      if (!ascending) {
        ordering = ordering.reverse();
      }
      return ordering;
    }
  }

  static class AssigneeSortIssueProcessor extends TextSortIssueProcessor {
    @Override
    String sortField(IssueDto issueDto) {
      return issueDto.getAssignee();
    }
  }

  static class StatusSortIssueProcessor extends TextSortIssueProcessor {
    @Override
    String sortField(IssueDto issueDto) {
      return issueDto.getStatus();
    }
  }

  static class SeveritySortIssueProcessor implements IssueProcessor {
    @Override
    public Function sortFieldFunction() {
      return IssueDtoToSeverity.INSTANCE;
    }

    @Override
    public Ordering sortFieldOrdering(boolean ascending) {
      Ordering<Integer> ordering = Ordering.<Integer>natural().nullsLast();
      if (!ascending) {
        ordering = ordering.reverse();
      }
      return ordering;
    }
  }

  abstract static class DateSortRowProcessor implements IssueProcessor {
    @Override
    public Function sortFieldFunction() {
      return new Function<IssueDto, Date>() {
        @Override
        public Date apply(IssueDto issueDto) {
          return sortField(issueDto);
        }
      };
    }

    abstract Date sortField(IssueDto issueDto);

    @Override
    public Ordering sortFieldOrdering(boolean ascending) {
      Ordering<Date> ordering = Ordering.<Date>natural().nullsLast();
      if (!ascending) {
        ordering = ordering.reverse();
      }
      return ordering;
    }
  }

  static class CreationDateSortIssueProcessor extends DateSortRowProcessor {
    @Override
    Date sortField(IssueDto issueDto) {
      return issueDto.getIssueCreationDate();
    }
  }

  static class UpdateDateSortIssueProcessor extends DateSortRowProcessor {
    @Override
    Date sortField(IssueDto issueDto) {
      return issueDto.getIssueUpdateDate();
    }
  }

  static class CloseDateSortIssueProcessor extends DateSortRowProcessor {
    @Override
    Date sortField(IssueDto issueDto) {
      return issueDto.getIssueCloseDate();
    }
  }

  private enum IssueDtoToSeverity implements Function<IssueDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull IssueDto issueDto) {
      return Severity.ALL.indexOf(issueDto.getSeverity());
    }
  }
}
