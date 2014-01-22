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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.user.UserSession;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleTagsTest {

  @Mock
  private RuleTagLookup ruleTagLookup;

  @Mock
  private RuleTagOperations ruleTagOperations;

  private RuleTags ruleTags;

  @Before
  public void setUp() {
    ruleTags = new RuleTags(ruleTagLookup, ruleTagOperations);
  }

  @Test
  public void should_find_all_tags() {
    Collection<String> tags = ImmutableList.of("tag1", "tag2");
    when(ruleTagLookup.listAllTags()).thenReturn(tags);
    assertThat(ruleTags.listAllTags()).isEqualTo(tags);
    verify(ruleTagLookup).listAllTags();
  }

  @Test
  public void should_create_tag() {
    String tag = "polop";
    RuleTagDto newTag = mock(RuleTagDto.class);
    when(ruleTagOperations.create(eq(tag), any(UserSession.class))).thenReturn(newTag);
    assertThat(ruleTags.create(tag)).isEqualTo(newTag);
    verify(ruleTagOperations).create(eq(tag), any(UserSession.class));
  }
}
