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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A mask restricts access of a classloader to resources through inclusion and exclusion patterns.
 * By default all resources/classes are visible.
 * <p/>
 * Format of inclusion/exclusion patterns is the file path separated by slashes, for example
 * "org/foo/Bar.class" or "org/foo/config.xml". Wildcard patterns are not supported. Directories must end with
 * slash, for example "org/foo/" for excluding package org.foo and its sub-packages. Add the
 * exclusion "/" to exclude everything.
 *
 * @since 0.1
 */
public class Mask {

  private static final String ROOT = "/";

  /**
   * Accepts everything
   *
   * @since 1.1
   */
  public static final Mask ALL = Mask.builder().build();

  /**
   * Accepts nothing
   *
   * @since 1.1
   */
  public static final Mask NONE = Mask.builder().exclude(ROOT).build();

  private final Set<String> inclusions;
  private final Set<String> exclusions;

  private Mask(Builder builder) {
    this.inclusions = Collections.unmodifiableSet(builder.inclusions);
    this.exclusions = Collections.unmodifiableSet(builder.exclusions);
  }

  /**
   * Create a {@link Builder} for building immutable instances of {@link Mask}
   *
   * @since 1.1
   */
  public static Builder builder() {
    return new Builder();
  }

  public Set<String> getInclusions() {
    return inclusions;
  }

  public Set<String> getExclusions() {
    return exclusions;
  }

  boolean acceptClass(String classname) {
    if (inclusions.isEmpty() && exclusions.isEmpty()) {
      return true;
    }
    return acceptResource(classToResource(classname));
  }

  boolean acceptResource(String name) {
    boolean ok = true;
    if (!inclusions.isEmpty()) {
      ok = false;
      for (String include : inclusions) {
        if (matchPattern(name, include)) {
          ok = true;
          break;
        }
      }
    }
    if (ok) {
      for (String exclude : exclusions) {
        if (matchPattern(name, exclude)) {
          ok = false;
          break;
        }
      }
    }
    return ok;
  }

  private static boolean matchPattern(String name, String pattern) {
    return pattern.equals(ROOT) || (pattern.endsWith("/") && name.startsWith(pattern)) || pattern.equals(name);
  }

  private static String classToResource(String classname) {
    return classname.replace('.', '/') + ".class";
  }


  public static class Builder {
    private final Set<String> inclusions = new HashSet<>();
    private final Set<String> exclusions = new HashSet<>();

    private Builder() {
    }

    public Builder include(String path, String... others) {
      doInclude(path);
      for (String other : others) {
        doInclude(other);
      }
      return this;
    }

    public Builder exclude(String path, String... others) {
      doExclude(path);
      for (String other : others) {
        doExclude(other);
      }
      return this;
    }

    public Builder copy(Mask with) {
      this.inclusions.addAll(with.inclusions);
      this.exclusions.addAll(with.exclusions);
      return this;
    }

    public Builder merge(Mask with) {
      List<String> lowestIncludes = new ArrayList<>();

      if (inclusions.isEmpty()) {
        lowestIncludes.addAll(with.inclusions);
      } else if (with.inclusions.isEmpty()) {
        lowestIncludes.addAll(inclusions);
      } else {
        for (String include : inclusions) {
          for (String fromInclude : with.inclusions) {
            overlappingInclude(include, fromInclude)
              .ifPresent(lowestIncludes::add);
          }
        }
      }
      inclusions.clear();
      inclusions.addAll(lowestIncludes);
      exclusions.addAll(with.exclusions);
      return this;
    }

    private static Optional<String> overlappingInclude(String include, String fromInclude) {
      if (fromInclude.equals(include)) {
        return Optional.of(fromInclude);
      } else if (fromInclude.startsWith(include)) {
        return Optional.of(fromInclude);
      } else if (include.startsWith(fromInclude)) {
        return Optional.of(include);
      }
      return Optional.empty();
    }

    public Mask build() {
      return new Mask(this);
    }

    private void doInclude(@Nullable String path) {
      this.inclusions.add(validatePath(path));
    }

    private void doExclude(@Nullable String path) {
      this.exclusions.add(validatePath(path));
    }

    private static String validatePath(@Nullable String path) {
      if (path == null) {
        throw new IllegalArgumentException("Mask path must not be null");
      }
      if (path.startsWith("/") && path.length() > 1) {
        throw new IllegalArgumentException("Mask path must not start with slash: ");
      }
      if (path.contains("*")) {
        throw new IllegalArgumentException("Mask path is not a wildcard pattern and should not contain star characters (*): " + path);
      }
      return path;
    }
  }
}
