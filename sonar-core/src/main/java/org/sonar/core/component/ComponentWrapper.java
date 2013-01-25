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

import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Component;
import org.sonar.core.graph.GraphUtil;

import javax.annotation.Nullable;

public class ComponentWrapper<C extends Component<C>> extends ElementWrapper<Vertex> implements Component<C> {

  public String getKey() {
    return (String) element().getProperty("key");
  }

  public String getName() {
    return (String) element().getProperty("name");
  }

  public String getQualifier() {
    return (String) element().getProperty("qualifier");
  }

  public ComponentWrapper setKey(String s) {
    element().setProperty("key", s);
    return this;
  }

  public ComponentWrapper setName(@Nullable String s) {
    GraphUtil.setNullableProperty(element(), "name", s);
    return this;
  }

  public ComponentWrapper setQualifier(String s) {
    element().setProperty("qualifier", s);
    return this;
  }

  public void populate(C component) {
    setKey(component.getKey());
    setName(component.getName());
    setQualifier(component.getQualifier());
    if (component instanceof ResourceComponent) {
      element().setProperty("sid", ((ResourceComponent) component).getSnapshotId());
    }
  }
}