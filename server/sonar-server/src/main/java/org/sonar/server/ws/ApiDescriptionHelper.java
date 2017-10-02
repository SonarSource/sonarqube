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
package org.sonar.server.ws;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ApiDescriptionHelper {

  private ApiDescriptionHelper() {
    // This is a helper class, use the static methods
  }

  /**
   * Generates a list of permissions in html formatting.<br />
   * <h2>Example:</h2>
   *
   * <br>
   * Requires one of the following permissions:
   * <ul>
   * <li>'Administer System'</li>
   * <li>'Administer' rights on the specified project</li>
   * </ul>
   *
   * @param htmlFormattedPermissionTexts
   * @return
   */
  public static String listOfPermissions(String... htmlFormattedPermissionTexts) {
    return "<br>" +
      "Requires one of the following permissions:" +
      "<ul>" +
      Arrays.stream(htmlFormattedPermissionTexts).map(p -> "<li>" + p + "</li>").collect(Collectors.joining()) +
      "</ul>";
  }

}
