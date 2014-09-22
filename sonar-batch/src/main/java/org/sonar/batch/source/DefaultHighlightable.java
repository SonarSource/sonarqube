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
package org.sonar.batch.source;

import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import org.sonar.api.component.Component;
import org.sonar.api.source.Highlightable;
import org.sonar.batch.highlighting.SyntaxHighlightingDataBuilder;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataTypes;

/**
 * @since 3.6
 * @deprecated since 4.5 no more used in batch 2.0
 */
@Deprecated
public class DefaultHighlightable implements Highlightable {

  private final Component component;
  private final ComponentDataCache cache;
  private final SyntaxHighlightingDataBuilder builder;

  public DefaultHighlightable(Component component, ComponentDataCache cache) {
    this.component = component;
    this.cache = cache;
    this.builder = new SyntaxHighlightingDataBuilder();
  }

  @Override
  public HighlightingBuilder newHighlighting() {
    return new DefaultHighlightingBuilder(component.key(), cache, builder);
  }

  @Override
  public Component component() {
    return component;
  }

  public SyntaxHighlightingDataBuilder getHighlightingRules() {
    return builder;
  }

  private static class DefaultHighlightingBuilder implements HighlightingBuilder {

    private final SyntaxHighlightingDataBuilder builder;
    private String componentKey;
    private ComponentDataCache cache;

    public DefaultHighlightingBuilder(String componentKey, ComponentDataCache cache, SyntaxHighlightingDataBuilder builder) {
      this.componentKey = componentKey;
      this.cache = cache;
      this.builder = builder;
    }

    @Override
    public HighlightingBuilder highlight(int startOffset, int endOffset, String typeOfText) {
      TypeOfText type = org.sonar.api.batch.sensor.highlighting.TypeOfText.forCssClass(typeOfText);
      builder.registerHighlightingRule(startOffset, endOffset, type);
      return this;
    }

    @Override
    public void done() {
      cache.setData(componentKey, SnapshotDataTypes.SYNTAX_HIGHLIGHTING, builder.build());
    }
  }
}
