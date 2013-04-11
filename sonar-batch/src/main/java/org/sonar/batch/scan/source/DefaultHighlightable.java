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
package org.sonar.batch.scan.source;

import org.sonar.api.component.Component;
import org.sonar.api.scan.source.Highlightable;

/**
 * @since 3.6
 */
public class DefaultHighlightable implements Highlightable {

  private final Component component;
  private final SyntaxHighlightingCache syntaxHighlightingCache;
  private final SyntaxHighlightingRuleSet.Builder highlightingRulesBuilder;

  public DefaultHighlightable(Component component, SyntaxHighlightingCache syntaxHighlightingCache) {
    this.component = component;
    this.syntaxHighlightingCache = syntaxHighlightingCache;
    this.highlightingRulesBuilder = SyntaxHighlightingRuleSet.builder();
  }

  @Override
  public HighlightingBuilder newHighlighting() {
    return new DefaultHighlightingBuilder();
  }

  @Override
  public void highlightText(int startOffset, int endOffset, String typeOfText) {
    highlightingRulesBuilder.registerHighlightingRule(startOffset, endOffset, typeOfText);
  }

  @Override
  public Component component() {
    return component;
  }

  public SyntaxHighlightingRuleSet getHighlightingRules() {
    return highlightingRulesBuilder.build();
  }

  private class DefaultHighlightingBuilder implements HighlightingBuilder {

    @Override
    public HighlightingBuilder highlight(int startOffset, int endOffset, String typeOfText) {
      highlightingRulesBuilder.registerHighlightingRule(startOffset, endOffset, typeOfText);
      return this;
    }

    @Override
    public void done() {
      String serializedHighlightingRules = highlightingRulesBuilder.build().serializeAsString();
      syntaxHighlightingCache.registerSourceHighlighting(component().key(), serializedHighlightingRules);
    }
  }
}
