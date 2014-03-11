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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RuleTagLookupTest {

  @Test
  public void should_pass_to_es_impl() {
    ESRuleTags esRuleTags = mock(ESRuleTags.class);
    Collection<String> tags = ImmutableList.of("tag1", "tag2");
    when(esRuleTags.searchAllTags()).thenReturn(tags);
    assertThat(new RuleTagLookup(esRuleTags).listAllTags()).isEqualTo(tags);
    verify(esRuleTags).searchAllTags();
  }
}
