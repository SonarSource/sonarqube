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
package org.sonar.server.ui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultJRubyIssuesTest {

  IssueFinder finder = mock(IssueFinder.class);
  DefaultJRubyIssues facade = new DefaultJRubyIssues(finder);

  @Test
  public void test_find() throws Exception {
    facade.find(ImmutableMap.<String, Object>of("keys", Lists.newArrayList("ABCDE")));
    verify(finder).find(argThat(new ArgumentMatcher<IssueQuery>() {
      @Override
      public boolean matches(Object o) {
        return ((IssueQuery) o).keys().contains("ABCDE");
      }
    }));
  }

  @Test
  public void should_create_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("ABCDE1234"));
    map.put("severities", newArrayList("MAJOR", "MINOR"));
    map.put("statuses", newArrayList("CLOSED"));
    map.put("resolutions", newArrayList("FALSE-POSITIVE"));
    map.put("components", newArrayList("org.apache"));
    map.put("userLogins", newArrayList("marilyn"));
    map.put("assigneeLogins", newArrayList("joanna"));
    map.put("limit", 10);
    map.put("offset", 50);

    IssueQuery query = new DefaultJRubyIssues(finder).newQuery(map);
    assertThat(query.keys()).containsOnly("ABCDE1234");
    assertThat(query.severities()).containsExactly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.userLogins()).containsOnly("marilyn");
    assertThat(query.assigneeLogins()).containsOnly("joanna");
    assertThat(query.limit()).isEqualTo(10);
    assertThat(query.offset()).isEqualTo(50);
  }

}
