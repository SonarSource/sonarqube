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
package org.sonar.core.component;

import org.sonar.api.component.Component;
import org.sonar.core.graph.BeanVertex;

public class ComponentVertex extends BeanVertex implements Component {

  public String key() {
    return (String) getProperty("key");
  }

  public String name() {
    return (String) getProperty("name");
  }

  public String longName() {
    return (String) getProperty("longName");
  }

  public String qualifier() {
    return (String) getProperty("qualifier");
  }

  void copyFrom(Component component) {
    setProperty("key", component.key());
    setProperty("name", component.name());
    setProperty("longName", component.longName());
    setProperty("qualifier", component.qualifier());
    if (component instanceof ResourceComponent) {
      setProperty("sid", ((ResourceComponent) component).snapshotId());
      setProperty("rid", ((ResourceComponent) component).resourceId());
    }
  }

  @Override
  public String toString() {
    return key();
  }
}
