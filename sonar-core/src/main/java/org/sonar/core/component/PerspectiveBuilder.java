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
package org.sonar.core.component;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Perspective;

import javax.annotation.CheckForNull;

public abstract class PerspectiveBuilder<T extends Perspective> implements BatchComponent, ServerComponent {

  private final String perspectiveKey;
  private final Class<T> perspectiveClass;

  protected PerspectiveBuilder(String perspectiveKey, Class<T> perspectiveClass) {
    this.perspectiveKey = perspectiveKey;
    this.perspectiveClass = perspectiveClass;
  }

  protected String getPerspectiveKey() {
    return perspectiveKey;
  }

  protected Class<T> getPerspectiveClass() {
    return perspectiveClass;
  }

  @CheckForNull
  public abstract T load(ComponentVertex component);

  public abstract T create(ComponentVertex component);

  public abstract Object[] path();

}
