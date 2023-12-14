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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * @since 0.1
 */
public class ClassloaderBuilder {
  private final Map<String, ClassRealm> previouslyCreatedClassLoaders;

  private final Map<String, NewRealm> newRealmsByKey = new HashMap<>();

  public ClassloaderBuilder() {
    this(emptyList());
  }

  /**
   * Creates a new classloader builder that can use a collection of previously created
   * classloaders as parent or siblings when building the new classloaders.
   *
   * @param previouslyCreatedClassLoaders Collection of classloaders that can be used as a
   *                                      parent or sibling. Must be of type {@link ClassRealm}.
   */
  public ClassloaderBuilder(Collection<ClassLoader> previouslyCreatedClassLoaders) {
    this.previouslyCreatedClassLoaders = new HashMap<>();
    for (ClassLoader cl : previouslyCreatedClassLoaders) {
      if (!(cl instanceof ClassRealm)) {
        throw new IllegalArgumentException("classloader not of type ClassRealm: " + cl);
      }
      ClassRealm classRealm = (ClassRealm) cl;
      this.previouslyCreatedClassLoaders.put(classRealm.getKey(), classRealm);
    }
  }

  public enum LoadingOrder {
    /**
     * Order: siblings, then parent, then self
     */
    PARENT_FIRST(ParentFirstStrategy.INSTANCE),

    /**
     * Order: siblings, then self, then parent
     */
    SELF_FIRST(SelfFirstStrategy.INSTANCE);

    private final Strategy strategy;

    LoadingOrder(Strategy strategy) {
      this.strategy = strategy;
    }
  }

  /**
   * Wrapper of {@link ClassRealm} as long as associations are not fully
   * defined
   */
  private static class NewRealm {
    private final ClassRealm realm;

    // key of the optional parent classloader
    private String parentKey;

    private final List<String> siblingKeys = new ArrayList<>();
    private final Map<String, Mask> associatedMasks = new HashMap<>();

    private NewRealm(ClassRealm realm) {
      this.realm = realm;
    }
  }

  /**
   * Declares a new classloader based on system classloader.
   */
  public ClassloaderBuilder newClassloader(String key) {
    return newClassloader(key, getSystemClassloader());
  }

  /**
   * Declares a new classloader based on a given parent classloader. Key must be unique. An
   * {@link IllegalArgumentException} is thrown if the key is already referenced.
   * <p/>
   * Default loading order is {@link LoadingOrder#PARENT_FIRST}.
   */
  public ClassloaderBuilder newClassloader(final String key, final ClassLoader baseClassloader) {
    if (newRealmsByKey.containsKey(key)) {
      throw new IllegalStateException(String.format("The classloader '%s' already exists. Can not create it twice.", key));
    }
    if (previouslyCreatedClassLoaders.containsKey(key)) {
      throw new IllegalStateException(String.format("The classloader '%s' already exists in the list of previously created classloaders."
        + " Can not create it twice.", key));
    }
    ClassRealm realm = AccessController.<PrivilegedAction<ClassRealm>>doPrivileged(() -> new ClassRealm(key, baseClassloader));
    realm.setStrategy(LoadingOrder.PARENT_FIRST.strategy);
    newRealmsByKey.put(key, new NewRealm(realm));
    return this;
  }

  public ClassloaderBuilder setParent(String key, String parentKey, Mask mask) {
    NewRealm newRealm = getOrFail(key);
    newRealm.parentKey = parentKey;
    newRealm.associatedMasks.put(parentKey, mask);
    return this;
  }

  public ClassloaderBuilder setParent(String key, ClassLoader parent, Mask mask) {
    NewRealm newRealm = getOrFail(key);
    newRealm.realm.setParent(new DefaultClassloaderRef(parent, mask));
    return this;
  }

  public ClassloaderBuilder addSibling(String key, String siblingKey, Mask mask) {
    NewRealm newRealm = getOrFail(key);
    newRealm.siblingKeys.add(siblingKey);
    newRealm.associatedMasks.put(siblingKey, mask);
    return this;
  }

  public ClassloaderBuilder addSibling(String key, ClassLoader sibling, Mask mask) {
    NewRealm newRealm = getOrFail(key);
    newRealm.realm.addSibling(new DefaultClassloaderRef(sibling, mask));
    return this;
  }

  public ClassloaderBuilder addURL(String key, URL url) {
    getOrFail(key).realm.addConstituent(url);
    return this;
  }

  public ClassloaderBuilder setMask(String key, Mask mask) {
    getOrFail(key).realm.setMask(mask);
    return this;
  }

  public ClassloaderBuilder setExportMask(String key, Mask mask) {
    getOrFail(key).realm.setExportMask(mask);
    return this;
  }

  public ClassloaderBuilder setLoadingOrder(String key, LoadingOrder order) {
    getOrFail(key).realm.setStrategy(order.strategy);
    return this;
  }

  /**
   * Returns the new classloaders, grouped by keys. The parent and sibling classloaders
   * that are already existed (see {@link #setParent(String, ClassLoader, Mask)}
   * and {@link #addSibling(String, ClassLoader, Mask)} are not included into result.
   */
  public Map<String, ClassLoader> build() {
    Map<String, ClassLoader> result = new HashMap<>();

    // all the classloaders are created. Associations can now be resolved.
    for (Map.Entry<String, NewRealm> entry : newRealmsByKey.entrySet()) {
      NewRealm newRealm = entry.getValue();
      if (newRealm.parentKey != null) {
        ClassRealm parent = getNewOrPreviousClassloader(newRealm.parentKey);
        Mask parentMask = newRealm.associatedMasks.get(newRealm.parentKey);
        parentMask = mergeWithExportMask(parentMask, newRealm.parentKey);
        newRealm.realm.setParent(new DefaultClassloaderRef(parent, parentMask));
      }
      for (String siblingKey : newRealm.siblingKeys) {
        ClassRealm sibling = getNewOrPreviousClassloader(siblingKey);
        Mask siblingMask = newRealm.associatedMasks.get(siblingKey);
        siblingMask = mergeWithExportMask(siblingMask, siblingKey);
        newRealm.realm.addSibling(new DefaultClassloaderRef(sibling, siblingMask));
      }
      result.put(newRealm.realm.getKey(), newRealm.realm);
    }
    return result;
  }

  private Mask mergeWithExportMask(Mask mask, String exportKey) {
    NewRealm newRealm = newRealmsByKey.get(exportKey);
    if (newRealm != null) {
      return Mask.builder().copy(mask).merge(newRealm.realm.getExportMask()).build();
    }
    ClassRealm realm = previouslyCreatedClassLoaders.get(exportKey);
    if (realm != null) {
      return Mask.builder().copy(mask).merge(realm.getExportMask()).build();
    }
    return mask;
  }

  private NewRealm getOrFail(String key) {
    NewRealm newRealm = newRealmsByKey.get(key);
    if (newRealm == null) {
      throw new IllegalStateException(String.format("The classloader '%s' does not exist", key));
    }
    return newRealm;
  }

  private ClassRealm getNewOrPreviousClassloader(String key) {
    NewRealm newRealm = newRealmsByKey.get(key);
    if (newRealm != null) {
      return newRealm.realm;
    }
    ClassRealm previousClassloader = previouslyCreatedClassLoaders.get(key);
    if (previousClassloader != null) {
      return previousClassloader;
    }

    throw new IllegalStateException(String.format("The classloader '%s' does not exist", key));
  }

  /**
   * JRE system classloader. In Oracle JVM:
   * - ClassLoader.getSystemClassLoader() is sun.misc.Launcher$AppClassLoader. It contains app classpath.
   * - ClassLoader.getSystemClassLoader().getParent() is sun.misc.Launcher$ExtClassLoader. It is the JRE core classloader.
   */
  private static ClassLoader getSystemClassloader() {
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    ClassLoader systemParent = systemClassLoader.getParent();
    if (systemParent != null) {
      systemClassLoader = systemParent;
    }
    return systemClassLoader;
  }
}
