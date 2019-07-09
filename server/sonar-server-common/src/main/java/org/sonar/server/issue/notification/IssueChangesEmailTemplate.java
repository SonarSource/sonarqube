/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.notification;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.sonar.api.config.EmailSettings;
import org.sonar.core.i18n.I18n;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;

import static java.net.URLEncoder.encode;
import static org.sonar.core.util.stream.MoreCollectors.index;

public abstract class IssueChangesEmailTemplate implements EmailTemplate {

  private static final Comparator<Rule> RULE_COMPARATOR = Comparator.comparing(r -> r.getKey().toString());
  private static final Comparator<Project> PROJECT_COMPARATOR = Comparator.comparing(Project::getProjectName)
    .thenComparing(t -> t.getBranchName().orElse(""));
  private static final Comparator<ChangedIssue> CHANGED_ISSUE_KEY_COMPARATOR = Comparator.comparing(ChangedIssue::getKey, Comparator.naturalOrder());
  /**
   * Assuming:
   * <ul>
   *   <li>UUID length of 40 chars</li>
   *   <li>a max URL length of 2083 chars</li>
   * </ul>
   * This leaves ~850 chars for the rest of the URL (including other parameters such as the project key and the branch),
   * which is reasonable to stay safe from the max URL length supported by some browsers and network devices.
   */
  private static final int MAX_ISSUES_BY_LINK = 40;
  private static final String URL_ENCODED_COMMA = urlEncode(",");

  private final I18n i18n;
  private final EmailSettings settings;

  protected IssueChangesEmailTemplate(I18n i18n, EmailSettings settings) {
    this.i18n = i18n;
    this.settings = settings;
  }

  /**
   * Adds "projectName" or "projectName, branchName" if branchName is non null
   */
  protected static void toString(StringBuilder sb, Project project) {
    Optional<String> branchName = project.getBranchName();
    if (branchName.isPresent()) {
      sb.append(project.getProjectName()).append(", ").append(branchName.get());
    } else {
      sb.append(project.getProjectName());
    }
  }

  static String toUrlParams(Project project) {
    return "id=" + urlEncode(project.getKey()) +
      project.getBranchName().map(branchName -> "&branch=" + urlEncode(branchName)).orElse("");
  }

  void addIssuesByProjectThenRule(StringBuilder sb, SetMultimap<Project, ChangedIssue> issuesByProject) {
    issuesByProject.keySet().stream()
      .sorted(PROJECT_COMPARATOR)
      .forEach(project -> {
        String encodedProjectParams = toUrlParams(project);
        paragraph(sb, s -> toString(s, project));
        addIssuesByRule(sb, issuesByProject.get(project), projectIssuePageHref(encodedProjectParams));
      });
  }

  void addIssuesByRule(StringBuilder sb, Collection<ChangedIssue> changedIssues, BiConsumer<StringBuilder, Collection<ChangedIssue>> issuePageHref) {
    ListMultimap<Rule, ChangedIssue> issuesByRule = changedIssues.stream()
      .collect(index(ChangedIssue::getRule, t -> t));

    Iterator<Rule> rules = issuesByRule.keySet().stream()
      .sorted(RULE_COMPARATOR)
      .iterator();
    if (!rules.hasNext()) {
      return;
    }

    sb.append("<ul>");
    while (rules.hasNext()) {
      Rule rule = rules.next();
      Collection<ChangedIssue> issues = issuesByRule.get(rule);

      sb.append("<li>").append("Rule ").append(" <em>").append(rule.getName()).append("</em> - ");
      appendIssueLinks(sb, issuePageHref, issues);
      sb.append("</li>");
    }
    sb.append("</ul>");
  }

  private static void appendIssueLinks(StringBuilder sb, BiConsumer<StringBuilder, Collection<ChangedIssue>> issuePageHref, Collection<ChangedIssue> issues) {
    SortedSet<ChangedIssue> sortedIssues = ImmutableSortedSet.copyOf(CHANGED_ISSUE_KEY_COMPARATOR, issues);
    int issueCount = issues.size();
    if (issueCount == 1) {
      link(sb, s -> issuePageHref.accept(s, sortedIssues), s -> s.append("See the single issue"));
    } else if (issueCount <= MAX_ISSUES_BY_LINK) {
      link(sb, s -> issuePageHref.accept(s, sortedIssues), s -> s.append("See all ").append(issueCount).append(" issues"));
    } else {
      sb.append("See issues");
      List<List<ChangedIssue>> issueGroups = Lists.partition(ImmutableList.copyOf(sortedIssues), MAX_ISSUES_BY_LINK);
      Iterator<List<ChangedIssue>> issueGroupsIterator = issueGroups.iterator();
      int[] groupIndex = new int[] {0};
      while (issueGroupsIterator.hasNext()) {
        List<ChangedIssue> issueGroup = issueGroupsIterator.next();
        sb.append(' ');
        link(sb, s -> issuePageHref.accept(s, issueGroup), issueGroupLabel(sb, groupIndex, issueGroup));
        groupIndex[0]++;
      }
    }
  }

  BiConsumer<StringBuilder, Collection<ChangedIssue>> projectIssuePageHref(String projectParams) {
    return (s, issues) -> {
      s.append(settings.getServerBaseURL()).append("/project/issues?").append(projectParams)
        .append("&issues=");

      Iterator<ChangedIssue> issueIterator = issues.iterator();
      while (issueIterator.hasNext()) {
        s.append(urlEncode(issueIterator.next().getKey()));
        if (issueIterator.hasNext()) {
          s.append(URL_ENCODED_COMMA);
        }
      }

      if (issues.size() == 1) {
        s.append("&open=").append(urlEncode(issues.iterator().next().getKey()));
      }
    };
  }

  private static Consumer<StringBuilder> issueGroupLabel(StringBuilder sb, int[] groupIndex, List<ChangedIssue> issueGroup) {
    return s -> {
      int firstIssueNumber = (groupIndex[0] * MAX_ISSUES_BY_LINK) + 1;
      if (issueGroup.size() == 1) {
        sb.append(firstIssueNumber);
      } else {
        sb.append(firstIssueNumber).append("-").append(firstIssueNumber + issueGroup.size() - 1);
      }
    };
  }

  void addFooter(StringBuilder sb, String notificationI18nKey) {
    paragraph(sb, s -> s.append("&nbsp;"));
    paragraph(sb, s -> {
      s.append("<small>");
      s.append("You received this email because you are subscribed to ")
        .append('"').append(i18n.message(Locale.ENGLISH, notificationI18nKey, notificationI18nKey)).append('"')
        .append(" notifications from SonarQube.");
      s.append(" Click ");
      link(s, s1 -> s1.append(settings.getServerBaseURL()).append("/account/notifications"), s1 -> s1.append("here"));
      s.append(" to edit your email preferences.");
      s.append("</small>");
    });
  }

  protected static void paragraph(StringBuilder sb, Consumer<StringBuilder> content) {
    sb.append("<p>");
    content.accept(sb);
    sb.append("</p>");
  }

  protected static void link(StringBuilder sb, Consumer<StringBuilder> link, Consumer<StringBuilder> content) {
    sb.append("<a href=\"");
    link.accept(sb);
    sb.append("\">");
    content.accept(sb);
    sb.append("</a>");
  }

  private static String urlEncode(String str) {
    try {
      return encode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

}
