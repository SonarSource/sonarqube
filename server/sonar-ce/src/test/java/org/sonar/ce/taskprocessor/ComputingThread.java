/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.taskprocessor;

class ComputingThread extends Thread {
  private boolean kill = false;

  public ComputingThread(String name) {
    setName(name);
  }

  private long fibo(int i) {
    if (kill) {
      return i;
    }
    if (i == 0) {
      return 0;
    }
    if (i == 1) {
      return 1;
    }
    return fibo(i - 1) + fibo(i - 2);
  }

  @Override
  public void run() {
    for (int i = 2; i < 50; i++) {
      fibo(i);
      if (kill) {
        break;
      }
    }
  }

  public void kill() {
    this.kill = true;
  }
}
