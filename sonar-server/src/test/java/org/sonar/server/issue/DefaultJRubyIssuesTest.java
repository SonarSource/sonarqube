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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DefaultJRubyIssuesTest {

  IssueFinder finder = mock(IssueFinder.class);
  ServerIssueChanges changes = mock(ServerIssueChanges.class);
  DefaultJRubyIssues facade = new DefaultJRubyIssues(finder, changes);

  @Test
  public void test_find() throws Exception {
    facade.find(ImmutableMap.<String, Object>of("keys", Lists.newArrayList("ABCDE")), 123);
    verify(finder).find(argThat(new ArgumentMatcher<IssueQuery>() {
      @Override
      public boolean matches(Object o) {
        return ((IssueQuery) o).keys().contains("ABCDE");
      }
    }), eq(123), eq(UserRole.CODEVIEWER));
  }

  @Test
  public void should_create_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("ABCDE1234"));
    map.put("severities", newArrayList("MAJOR", "MINOR"));
    map.put("statuses", newArrayList("CLOSED"));
    map.put("resolutions", newArrayList("FALSE-POSITIVE"));
    map.put("components", newArrayList("org.apache"));
    map.put("componentRoots", newArrayList("org.sonar"));
    map.put("userLogins", newArrayList("marilyn"));
    map.put("assignees", newArrayList("joanna"));
    map.put("createdAfter", "2013-04-16T09:08:24+0200");
    map.put("createdBefore", "2013-04-17T09:08:24+0200");
    map.put("rules", "squid:AvoidCycle,findbugs:NullReference");
    map.put("limit", 10);
    map.put("offset", 50);

    IssueQuery query = new DefaultJRubyIssues(finder, changes).toQuery(map);
    assertThat(query.keys()).containsOnly("ABCDE1234");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.components()).containsOnly("org.apache");
    assertThat(query.componentRoots()).containsOnly("org.sonar");
    assertThat(query.userLogins()).containsOnly("marilyn");
    assertThat(query.assignees()).containsOnly("joanna");
    assertThat(query.rules()).hasSize(2);
    assertThat(query.createdAfter()).isEqualTo(DateUtils.parseDateTime("2013-04-16T09:08:24+0200"));
    assertThat(query.createdBefore()).isEqualTo(DateUtils.parseDateTime("2013-04-17T09:08:24+0200"));
    assertThat(query.limit()).isEqualTo(10);
    assertThat(query.offset()).isEqualTo(50);
  }

  @Test
  public void should_parse_list_of_rules() {
    assertThat(DefaultJRubyIssues.toRules(null)).isNull();
    assertThat(DefaultJRubyIssues.toRules("")).isEmpty();
    assertThat(DefaultJRubyIssues.toRules("squid:AvoidCycle")).containsOnly(RuleKey.of("squid", "AvoidCycle"));
    assertThat(DefaultJRubyIssues.toRules("squid:AvoidCycle,findbugs:NullRef")).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
    assertThat(DefaultJRubyIssues.toRules(asList("squid:AvoidCycle", "findbugs:NullRef"))).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
  }

  @Test
  public void should_parse_list_of_strings() {
    assertThat(DefaultJRubyIssues.toStrings(null)).isNull();
    assertThat(DefaultJRubyIssues.toStrings("")).isEmpty();
    assertThat(DefaultJRubyIssues.toStrings("foo")).containsOnly("foo");
    assertThat(DefaultJRubyIssues.toStrings("foo,bar")).containsOnly("foo", "bar");
    assertThat(DefaultJRubyIssues.toStrings(asList("foo", "bar"))).containsOnly("foo", "bar");

  }

  @Test
  public void should_start() throws Exception {
    facade.start();
    // nothing is done
    verifyZeroInteractions(finder);
  }
}
