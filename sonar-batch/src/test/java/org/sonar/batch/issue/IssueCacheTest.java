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
package org.sonar.batch.issue;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.Severity;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.CachesTest;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueCacheTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  Caches caches;

  @Before
  public void start() throws Exception {
    caches = CachesTest.createCacheOnTemp(temp);
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_add_new_issue() throws Exception {
    IssueCache cache = new IssueCache(caches);
    DefaultIssue issue1 = new DefaultIssue().setKey("111").setComponentKey("org.struts.Action");
    DefaultIssue issue2 = new DefaultIssue().setKey("222").setComponentKey("org.struts.Action");
    DefaultIssue issue3 = new DefaultIssue().setKey("333").setComponentKey("org.struts.Filter").setTags(Arrays.asList("foo", "bar"));
    cache.put(issue1).put(issue2).put(issue3);

    assertThat(issueKeys(cache.byComponent("org.struts.Action"))).containsOnly("111", "222");
    assertThat(issueKeys(cache.byComponent("org.struts.Filter"))).containsOnly("333");
    assertThat(cache.byComponent("org.struts.Filter").iterator().next().tags()).containsOnly("foo", "bar");
  }

  @Test
  public void should_update_existing_issue() throws Exception {
    IssueCache cache = new IssueCache(caches);
    DefaultIssue issue = new DefaultIssue().setKey("111").setComponentKey("org.struts.Action").setSeverity(Severity.BLOCKER);
    cache.put(issue);

    issue.setSeverity(Severity.MINOR);
    cache.put(issue);

    List<DefaultIssue> issues = ImmutableList.copyOf(cache.byComponent("org.struts.Action"));
    assertThat(issues).hasSize(1);
    Issue reloaded = issues.iterator().next();
    assertThat(reloaded.key()).isEqualTo("111");
    assertThat(reloaded.severity()).isEqualTo(Severity.MINOR);
  }

  @Test
  public void should_get_all_issues() throws Exception {
    IssueCache cache = new IssueCache(caches);
    DefaultIssue issue1 = new DefaultIssue().setKey("111").setComponentKey("org.struts.Action").setSeverity(Severity.BLOCKER);
    DefaultIssue issue2 = new DefaultIssue().setKey("222").setComponentKey("org.struts.Filter").setSeverity(Severity.INFO);
    cache.put(issue1).put(issue2);

    List<DefaultIssue> issues = ImmutableList.copyOf(cache.all());
    assertThat(issues).containsOnly(issue1, issue2);
  }

  private Collection<String> issueKeys(Iterable<DefaultIssue> issues) {
    return Collections2.transform(ImmutableList.copyOf(issues), new Function<DefaultIssue, String>() {
      @Override
      public String apply(@Nullable DefaultIssue issue) {
        return issue.key();
      }
    });
  }
}
