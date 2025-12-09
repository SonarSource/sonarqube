/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.es;

import java.util.Set;

/**
 * This kind of indexers get initialized during web server startup.
 */
public interface StartupIndexer {
  enum Type {
    SYNCHRONOUS, ASYNCHRONOUS
  }

  default Type getType() {
    return Type.SYNCHRONOUS;
  }

  default void triggerAsyncIndexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    throw new IllegalStateException("ASYNCHRONOUS StartupIndexer must implement initAsyncIndexOnStartup");
  }

  /**
   * This reindexing method will only be called on startup, and only,
   * if there is at least one uninitialized type.
   */
  default void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    throw new IllegalStateException("SYNCHRONOUS StartupIndexer must implement indexOnStartup");
  }

  Set<IndexType> getIndexTypes();

}
