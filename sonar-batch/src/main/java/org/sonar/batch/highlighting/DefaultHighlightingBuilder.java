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
package org.sonar.batch.highlighting;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataTypes;

public class DefaultHighlightingBuilder implements HighlightingBuilder {

  private final SyntaxHighlightingDataBuilder builder;
  private String componentKey;
  private ComponentDataCache cache;
  private boolean done = false;

  public DefaultHighlightingBuilder(String componentKey, ComponentDataCache cache) {
    this.componentKey = componentKey;
    this.cache = cache;
    this.builder = new SyntaxHighlightingDataBuilder();
  }

  @Override
  public HighlightingBuilder highlight(int startOffset, int endOffset, TypeOfText typeOfText) {
    Preconditions.checkState(!done, "done() already called");
    builder.registerHighlightingRule(startOffset, endOffset, typeOfText);
    return this;
  }

  @Override
  public void done() {
    Preconditions.checkState(!done, "done() already called");
    cache.setData(componentKey, SnapshotDataTypes.SYNTAX_HIGHLIGHTING, builder.build());
  }
}
