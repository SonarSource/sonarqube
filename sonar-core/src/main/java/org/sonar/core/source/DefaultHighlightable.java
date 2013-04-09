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

import org.sonar.api.component.Component;
import org.sonar.api.scan.source.Highlightable;
import org.sonar.api.scan.source.SyntaxHighlightingRuleSet;

/**
 * @since 3.6
 */
public class DefaultHighlightable implements Highlightable {

  private final SyntaxHighlightingRuleSet.Builder highlightingRulesBuilder;

  public DefaultHighlightable() {
    highlightingRulesBuilder = SyntaxHighlightingRuleSet.builder();
  }

  @Override
  public void highlightText(int startOffset, int endOffset, String typeOfText) {
    highlightingRulesBuilder.registerHighlightingRule(startOffset, endOffset, typeOfText);
  }

  @Override
  public Component component() {
    throw new UnsupportedOperationException("Unexpected call to component API");
  }

  public SyntaxHighlightingRuleSet getHighlightingRules() {
    return highlightingRulesBuilder.build();
  }
}
