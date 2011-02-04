/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.util.AbstractMap;
import java.util.Set;

/**
 * See http://java.sun.com/docs/books/tutorial/java/javaOO/nested.html
 */
public class WithAnonymousClass {

  public void mainMethod1() {
    AbstractMap anonymousMap = new AbstractMap() {
      public Set entrySet() {
        return null;
      }
    };
    anonymousMap.entrySet();
    anonymousMap = null;
  }

  public void mainMethod2() {
    int i = 9999;
    i++;
    i += 444;
  }


}