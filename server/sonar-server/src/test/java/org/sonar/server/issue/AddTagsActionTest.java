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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.core.issue.DefaultIssue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddTagsActionTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  private IssueFieldsSetter issueUpdater = new IssueFieldsSetter();
  private AddTagsAction underTest = new AddTagsAction(issueUpdater);

  @Test
  @SuppressWarnings("unchecked")
  public void should_execute() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("tags", "tag2,tag3");

    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.tags()).thenReturn(ImmutableSet.of("tag1", "tag3"));

    Action.Context context = mock(Action.Context.class, Mockito.RETURNS_DEEP_STUBS);
    when(context.issue()).thenReturn(issue);

    underTest.execute(properties, context);
    verify(issue).setTags(ImmutableSet.of("tag1", "tag2", "tag3"));
  }

  @Test
  public void should_fail_if_tag_is_not_valid() {
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Tag 'th ag' is invalid. Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'");

    Map<String, Object> properties = new HashMap<>();
    properties.put("tags", "th ag");

    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.tags()).thenReturn(ImmutableSet.of("tag1", "tag3"));

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    underTest.execute(properties, context);
  }
}
