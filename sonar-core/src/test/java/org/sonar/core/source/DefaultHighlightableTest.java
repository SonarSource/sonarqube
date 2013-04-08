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
package org.sonar.core.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultHighlightableTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test
  public void should_register_highlighting_rule() throws Exception {

    DefaultHighlightable highlightable = new DefaultHighlightable();
    highlightable.highlightText(1, 10, "k");

    assertThat(highlightable.getHighlightingRules().getSyntaxHighlightingRuleSet()).hasSize(1);
  }

  @Test
  public void should_reject_any_call_to_component() throws Exception {

    throwable.expect(UnsupportedOperationException.class);

    DefaultHighlightable highlightable = new DefaultHighlightable();
    highlightable.component();
  }
}
