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
package org.sonar.batch.bootstrapper;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * Special {@link URLClassLoader} to execute Sonar, which restricts loading from parent.
 */
public class BootstrapClassLoader extends URLClassLoader {

  private String[] unmaskedPackages;

  public BootstrapClassLoader(ClassLoader parent, String... unmaskedPackages) {
    super(new URL[0], parent);
    this.unmaskedPackages = unmaskedPackages;
  }

  /**
   * {@inheritDoc} Visibility of a method has been relaxed to public.
   */
  @Override
  public void addURL(URL url) {
    super.addURL(url);
  }

  /**
   * {@inheritDoc} Visibility of a method has been relaxed to public.
   */
  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    return super.findClass(name);
  }

  /**
   * @return true, if class can be loaded from parent ClassLoader
   */
  boolean canLoadFromParent(String name) {
    for (String pkg : unmaskedPackages) {
      if (name.startsWith(pkg + ".")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Same behavior as in {@link URLClassLoader#loadClass(String, boolean)}, except loading from parent.
   */
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      try {
        // Load from parent
        if ((getParent() != null) && canLoadFromParent(name)) {
          c = getParent().loadClass(name);
        } else {
          // Load from system
          c = getSystemClassLoader().loadClass(name);
        }
      } catch (ClassNotFoundException e) {
        // If still not found, then invoke findClass in order
        // to find the class.
        c = findClass(name);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

  /**
   * Unlike {@link URLClassLoader#getResource(String)} don't return resource from parent.
   * See http://jira.codehaus.org/browse/SONAR-2276
   */
  @Override
  public URL getResource(String name) {
    return findResource(name);
  }

  /**
   * Unlike {@link URLClassLoader#getResources(String)} don't return resources from parent.
   * See http://jira.codehaus.org/browse/SONAR-2276
   */
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    return findResources(name);
  }

}
