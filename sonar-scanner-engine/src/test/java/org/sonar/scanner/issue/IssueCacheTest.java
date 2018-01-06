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
package org.sonar.scanner.issue;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.scanner.index.AbstractCachesTest;
import org.sonar.scanner.issue.tracking.TrackedIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueCacheTest extends AbstractCachesTest {

  @Test
  public void should_add_new_issue() {
    IssueCache cache = new IssueCache(caches);
    TrackedIssue issue1 = createIssue("111", "org.struts.Action", null);
    TrackedIssue issue2 = createIssue("222", "org.struts.Action", null);
    TrackedIssue issue3 = createIssue("333", "org.struts.Filter", null);
    issue3.setAssignee("foo");
    cache.put(issue1).put(issue2).put(issue3);

    assertThat(issueKeys(cache.byComponent("org.struts.Action"))).containsOnly("111", "222");
    assertThat(issueKeys(cache.byComponent("org.struts.Filter"))).containsOnly("333");
    assertThat(cache.byComponent("org.struts.Filter").iterator().next().assignee()).isEqualTo("foo");
  }

  @Test
  public void should_update_existing_issue() {
    IssueCache cache = new IssueCache(caches);
    TrackedIssue issue = createIssue("111", "org.struts.Action", Severity.BLOCKER);
    cache.put(issue);

    issue.setSeverity(Severity.MINOR);
    cache.put(issue);

    List<TrackedIssue> issues = ImmutableList.copyOf(cache.byComponent("org.struts.Action"));
    assertThat(issues).hasSize(1);
    TrackedIssue reloaded = issues.iterator().next();
    assertThat(reloaded.key()).isEqualTo("111");
    assertThat(reloaded.severity()).isEqualTo(Severity.MINOR);
  }

  @Test
  public void should_get_all_issues() {
    IssueCache cache = new IssueCache(caches);
    TrackedIssue issue1 = createIssue("111", "org.struts.Action", Severity.BLOCKER);
    TrackedIssue issue2 = createIssue("222", "org.struts.Filter", Severity.INFO);
    cache.put(issue1).put(issue2);

    List<TrackedIssue> issues = ImmutableList.copyOf(cache.all());
    assertThat(issues).containsOnly(issue1, issue2);
  }

  private Collection<String> issueKeys(Iterable<TrackedIssue> issues) {
    return Collections2.transform(ImmutableList.copyOf(issues), new Function<TrackedIssue, String>() {
      @Override
      public String apply(@Nullable TrackedIssue issue) {
        return issue.key();
      }
    });
  }

  private TrackedIssue createIssue(String key, String componentKey, String severity) {
    TrackedIssue issue = new TrackedIssue();
    issue.setKey(key);
    issue.setComponentKey(componentKey);
    issue.setSeverity(severity);

    return issue;
  }
}
