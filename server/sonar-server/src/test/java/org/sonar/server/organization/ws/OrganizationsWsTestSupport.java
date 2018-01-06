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
package org.sonar.server.organization.ws;

import javax.annotation.Nullable;
import org.sonar.server.ws.TestRequest;

public class OrganizationsWsTestSupport {

  static final String STRING_65_CHARS_LONG = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz1234567890-ab";
  static final String STRING_257_CHARS_LONG = String.format("%1$257.257s", "a").replace(" ", "a");

  static void setParam(TestRequest request, String param, @Nullable String value) {
    if (value != null) {
      request.setParam(param, value);
    }
  }
}
