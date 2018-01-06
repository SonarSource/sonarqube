/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.process;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Calls {@link System#exit(int)} except from shutdown hooks, to prevent
 * deadlocks. See http://stackoverflow.com/a/19552359/229031
 */
public class SystemExit {

  private final AtomicBoolean inShutdownHook = new AtomicBoolean(false);

  public void exit(int code) {
    if (!inShutdownHook.get()) {
      doExit(code);
    }
  }

  public boolean isInShutdownHook() {
    return inShutdownHook.get();
  }

  /**
   * Declarative approach. I don't know how to get this lifecycle state from Java API.
   */
  public void setInShutdownHook() {
    inShutdownHook.set(true);
  }

  void doExit(int code) {
    System.exit(code);
  }
}
