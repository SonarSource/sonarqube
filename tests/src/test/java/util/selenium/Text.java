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
package util.selenium;

import com.google.common.base.Joiner;
import org.openqa.selenium.By;

public abstract class Text {
  private Text() {
    // Static utility class
  }

  public static String doesOrNot(boolean not, String verb) {
    if (!verb.contains(" ")) {
      if (not) {
        return "doesn't " + verb;
      } else if (verb.endsWith("h")) {
        return verb + "es";
      } else {
        return verb + "s";
      }
    }

    String[] verbs = verb.split(" ");
    verbs[0] = doesOrNot(not, verbs[0]);

    return Joiner.on(" ").join(verbs);
  }

  public static String isOrNot(boolean not, String state) {
    return (not ? "is not " : "is ") + state;
  }

  public static String plural(int n, String word) {
    return (n + " " + word) + (n <= 1 ? "" : "s");
  }

  public static String toString(By selector) {
    return selector.toString().replace("By.selector: ", "").replace("By.cssSelector: ", "");
  }
}
