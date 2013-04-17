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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.batch.index.Caches;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nullable;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class IssueCacheTest {

  Caches caches = new Caches();

  @Before
  public void start() {
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_add_new_issue() throws Exception {
    IssueCache cache = new IssueCache();
    DefaultIssue issue1 = new DefaultIssue().setKey("111").setComponentKey("org.struts.Action");
    DefaultIssue issue2 = new DefaultIssue().setKey("222").setComponentKey("org.struts.Action");
    DefaultIssue issue3 = new DefaultIssue().setKey("333").setComponentKey("org.struts.Filter");
    cache.addOrUpdate(issue1).addOrUpdate(issue2).addOrUpdate(issue3);

    assertThat(issueKeys(cache.componentIssues("org.struts.Action"))).containsOnly("111", "222");
    assertThat(issueKeys(cache.componentIssues("org.struts.Filter"))).containsOnly("333");
  }

  @Test
  public void should_update_existing_issue() throws Exception {
    IssueCache cache = new IssueCache();
    DefaultIssue issue = new DefaultIssue().setKey("111").setComponentKey("org.struts.Action").setSeverity(Severity.BLOCKER);
    cache.addOrUpdate(issue);

    issue.setSeverity(Severity.MINOR);
    cache.addOrUpdate(issue);

    Collection<Issue> issues = cache.componentIssues("org.struts.Action");
    assertThat(issues).hasSize(1);
    Issue reloaded = issues.iterator().next();
    assertThat(reloaded.key()).isEqualTo("111");
    assertThat(reloaded.severity()).isEqualTo(Severity.MINOR);
  }

  Collection<String> issueKeys(Collection<Issue> issues) {
    return Collections2.transform(issues, new Function<Issue, String>() {
      @Override
      public String apply(@Nullable Issue issue) {
        return issue.key();
      }
    });
  }
}
