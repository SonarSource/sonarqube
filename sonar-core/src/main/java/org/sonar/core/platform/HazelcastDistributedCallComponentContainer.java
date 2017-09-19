/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.core.platform;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Helper class, that makes some components available statically, so that they can be used
 * when a distributed call is sent to cluster members of both web and ce type.
 */
public class HazelcastDistributedCallComponentContainer {
  private static Supplier<ComponentContainer> provider;

  private HazelcastDistributedCallComponentContainer() {
  }

  public static void put(Supplier<ComponentContainer> provider) {
    HazelcastDistributedCallComponentContainer.provider = provider;
  }

  public static ComponentContainer get() {
    return requireNonNull(provider, "The ComponentContainer has not yet been registered").get();
  }
}
