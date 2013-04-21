/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.source;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.scan.source.Highlightable;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.component.PerspectiveBuilder;
import org.sonar.core.component.ResourceComponent;

import javax.annotation.CheckForNull;

import java.util.Set;

/**
 * @since 3.6
 */
public class HighlightableBuilder extends PerspectiveBuilder<Highlightable> {

  private static final Set<String> SUPPORTED_QUALIFIERS = ImmutableSet.of(Qualifiers.FILE, Qualifiers.CLASS, Qualifiers.UNIT_TEST_FILE);
  private final ComponentDataCache cache;

  public HighlightableBuilder(ComponentDataCache cache) {
    super(Highlightable.class);
    this.cache = cache;
  }

  @CheckForNull
  @Override
  protected Highlightable loadPerspective(Class<Highlightable> perspectiveClass, Component component) {
    boolean supported = SUPPORTED_QUALIFIERS.contains(component.qualifier());
    if (supported && component instanceof ResourceComponent) {
      // temporary hack waiting for the removal of JavaClass.
      // JavaClass has the same qualifier than JavaFile, so they have to distinguished by their scope
      supported = Scopes.FILE.equals(((ResourceComponent) component).scope());
    }
    if (supported) {
      return new DefaultHighlightable(component, cache);
    }
    return null;
  }
}
