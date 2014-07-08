/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.runner;

public class SonarProcess extends Thread {

  final String name;
  final int port;

  public SonarProcess(String name, int port) {
    this.name = name;
    this.port = port;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      System.out.println("Thread[" + name + "] - ping");
      try {
        if (getName().equalsIgnoreCase("ES")) {
          Thread.sleep(20000);
        }
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        break;
      }
    }
    System.out.println("Shutting down Thread[" + name + "]");
  }
}