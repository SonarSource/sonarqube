/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.newcodeperiod.ws;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

class NewCodeActionSupport {

  private static final Pattern pattern = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}).*$");

  private NewCodeActionSupport() {
    throw new UnsupportedOperationException("This class cannot be instantiated.");
  }

  protected static String getDocumentationUrl(@Nonnull String version, @Nonnull String docPath) {
    Matcher matcher = pattern.matcher(version);
    boolean isSnapshot = version.contains("-SNAPSHOT");
    boolean isPreviousVersion = matcher.matches() && matcher.groupCount() == 1 && !isSnapshot;
    String docVersion = isPreviousVersion ? matcher.group(1) : "latest";
    return String.format("https://docs.sonarqube.org/%s/%s", docVersion, docPath);
  }

}
