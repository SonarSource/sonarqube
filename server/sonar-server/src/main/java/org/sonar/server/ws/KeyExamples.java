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
package org.sonar.server.ws;

public class KeyExamples {
  public static final String KEY_FILE_EXAMPLE_001 = "my_project:/src/foo/Bar.php";
  public static final String KEY_FILE_EXAMPLE_002 = "another_project:/src/foo/Foo.php";
  public static final String KEY_PROJECT_EXAMPLE_001 = "my_project";
  public static final String KEY_PROJECT_EXAMPLE_002 = "another_project";
  public static final String KEY_PROJECT_EXAMPLE_003 = "third_project";

  public static final String KEY_ORG_EXAMPLE_001 = "my-org";
  public static final String KEY_ORG_EXAMPLE_002 = "foo-company";

  public static final String KEY_BRANCH_EXAMPLE_001 = "feature/my_branch";
  public static final String KEY_PULL_REQUEST_EXAMPLE_001 = "5461";

  public static final String NAME_WEBHOOK_EXAMPLE_001 = "My Webhook";
  public static final String URL_WEBHOOK_EXAMPLE_001 = "https://www.my-webhook-listener.com/sonar";

  private KeyExamples() {
    // prevent instantiation
  }
}
