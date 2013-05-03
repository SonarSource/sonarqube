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

package org.sonar.batch.source;

import org.sonar.api.component.Component;
import org.sonar.api.source.Symbolizable;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.component.PerspectiveBuilder;

public class SymbolizableBuilder extends PerspectiveBuilder<Symbolizable> {

  private final ComponentDataCache cache;

  public SymbolizableBuilder(ComponentDataCache cache) {
    super(Symbolizable.class);
    this.cache = cache;
  }

  @Override
  protected Symbolizable loadPerspective(Class<Symbolizable> perspectiveClass, Component component) {
    return new DefaultSymbolizable(cache, component);
  }
}
