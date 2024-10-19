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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.annotation.CheckForNull;

class ClassRealm extends URLClassLoader implements StrategyContext {

  private final String key;
  private Mask mask = Mask.ALL;
  private Mask exportMask = Mask.ALL;
  private ClassloaderRef parentRef = NullClassloaderRef.INSTANCE;
  private List<ClassloaderRef> siblingRefs = new ArrayList<>();
  private Strategy strategy;

  ClassRealm(String key, ClassLoader baseClassloader) {
    super(new URL[0], baseClassloader);
    this.key = key;
  }

  String getKey() {
    return key;
  }

  ClassRealm setMask(Mask mask) {
    this.mask = mask;
    return this;
  }

  Mask getExportMask() {
    return exportMask;
  }

  ClassRealm setExportMask(Mask exportMask) {
    this.exportMask = exportMask;
    return this;
  }

  ClassRealm setParent(ClassloaderRef parentRef) {
    this.parentRef = parentRef;
    return this;
  }

  ClassRealm addSibling(ClassloaderRef ref) {
    this.siblingRefs.add(ref);
    return this;
  }

  ClassRealm setStrategy(Strategy strategy) {
    this.strategy = strategy;
    return this;
  }

  ClassRealm addConstituent(URL url) {
    super.addURL(url);
    return this;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (mask.acceptClass(name)) {
      try {
        // first, try loading bootstrap classes
        return super.loadClass(name, resolve);
      } catch (ClassNotFoundException ignored) {
        // next, try loading via siblings, self and parent as controlled by strategy
        return strategy.loadClass(this, name);
      }
    }
    throw new ClassNotFoundException(String.format("Class %s is not accepted in classloader %s", name, this));
  }


  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    // not supposed to be used. Replaced by loadClassFromSelf(String)
    throw new ClassNotFoundException(name);
  }

  @CheckForNull
  @Override
  public URL getResource(String name) {
    if (mask.acceptResource(name)) {
      return strategy.getResource(this, name);
    }
    return null;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    // Important note: do not use java.util.Set as equals and hashCode methods of
    // java.net.URL perform domain name resolution. This can result in a big performance hit.
    List<URL> resources = new ArrayList<>();
    if (mask.acceptResource(name)) {
      strategy.getResources(this, name, resources);
    }
    return Collections.enumeration(resources);
  }

  @Override
  public Class<?> loadClassFromSelf(String name) {
    Class<?> clazz = findLoadedClass(name);
    if (clazz == null) {
      try {
        return super.findClass(name);
      } catch (ClassNotFoundException ignored) {
        // return null when class is not found, so that loading strategy
        // can try parent or sibling classloaders.
      }
    }
    return clazz;
  }

  @Override
  public Class<?> loadClassFromSiblings(String name) {
    for (ClassloaderRef siblingRef : siblingRefs) {
      Class<?> clazz = siblingRef.loadClassIfPresent(name);
      if (clazz != null) {
        return clazz;
      }
    }
    return null;
  }

  @Override
  public Class<?> loadClassFromParent(String name) {
    return parentRef.loadClassIfPresent(name);
  }

  @Override
  public URL loadResourceFromSelf(String name) {
    return super.findResource(name);
  }

  @Override
  public URL loadResourceFromSiblings(String name) {
    for (ClassloaderRef siblingRef : siblingRefs) {
      URL url = siblingRef.loadResourceIfPresent(name);
      if (url != null) {
        return url;
      }
    }
    return null;
  }

  @Override
  public URL loadResourceFromParent(String name) {
    return parentRef.loadResourceIfPresent(name);
  }

  @Override
  public void loadResourcesFromSelf(String name, Collection<URL> appendTo) {
    try {
      appendTo.addAll(Collections.list(super.findResources(name)));
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to load resources named '%s' from classloader %s", name, toString()), e);
    }
  }

  @Override
  public void loadResourcesFromSiblings(String name, Collection<URL> appendTo) {
    for (ClassloaderRef siblingRef : siblingRefs) {
      siblingRef.loadResources(name, appendTo);
    }
  }

  @Override
  public void loadResourcesFromParent(String name, Collection<URL> appendTo) {
    parentRef.loadResources(name, appendTo);
  }

  @Override
  public String toString() {
    return String.format("ClassRealm{%s}", key);
  }
}
