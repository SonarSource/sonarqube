/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

class DefaultClassloaderRef implements ClassloaderRef {
  private final Mask mask;
  private final ClassLoader classloader;

  DefaultClassloaderRef(ClassLoader classloader, Mask mask) {
    this.classloader = classloader;
    this.mask = mask;
  }

  @Override
  public Class loadClassIfPresent(String classname) {
    if (mask.acceptClass(classname)) {
      try {
        return classloader.loadClass(classname);
      } catch (ClassNotFoundException ignored) {
        // excepted behavior. Return null if class does not exist in this classloader
      }
    }
    return null;
  }

  @Override
  public URL loadResourceIfPresent(String name) {
    if (mask.acceptResource(name)) {
      return classloader.getResource(name);
    }
    return null;
  }

  @Override
  public void loadResources(String name, Collection<URL> appendTo) {
    if (mask.acceptResource(name)) {
      try {
        Enumeration<URL> resources = classloader.getResources(name);
        while (resources.hasMoreElements()) {
          appendTo.add(resources.nextElement());
        }
      } catch (IOException e) {
        throw new IllegalStateException(String.format("Fail to load resources named '%s'", name), e);
      }
    }
  }
}
