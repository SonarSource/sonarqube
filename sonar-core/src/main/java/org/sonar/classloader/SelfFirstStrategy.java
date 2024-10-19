/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.classloader;

import java.net.URL;
import java.util.Collection;

class SelfFirstStrategy implements Strategy {

  static final SelfFirstStrategy INSTANCE = new SelfFirstStrategy();

  private SelfFirstStrategy() {
    // singleton instance
  }

  @Override
  public Class<?> loadClass(StrategyContext context, String name) throws ClassNotFoundException {
    Class<?> clazz = context.loadClassFromSiblings(name);
    if (clazz == null) {
      clazz = context.loadClassFromSelf(name);
      if (clazz == null) {
        clazz = context.loadClassFromParent(name);
        if (clazz == null) {
          throw new ClassNotFoundException(name);
        }
      }
    }
    return clazz;
  }

  @Override
  public URL getResource(StrategyContext context, String name) {
    URL url = context.loadResourceFromSiblings(name);
    if (url == null) {
      url = context.loadResourceFromSelf(name);
      if (url == null) {
        url = context.loadResourceFromParent(name);
      }
    }
    return url;
  }

  @Override
  public void getResources(StrategyContext context, String name, Collection<URL> appendTo) {
    context.loadResourcesFromSiblings(name, appendTo);
    context.loadResourcesFromSelf(name, appendTo);
    context.loadResourcesFromParent(name, appendTo);
  }
}
