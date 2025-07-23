/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualityprofile;

import org.apache.commons.lang.StringUtils;
import org.sonarqube.ws.WsUtils;

import java.util.List;

class ImportedQProfile {
  private final String profileName;
  private final String profileLang;

  private final List<ImportedRule> rules;

  public ImportedQProfile(String profileName, String profileLang, List<ImportedRule> rules) {
    WsUtils.checkArgument(StringUtils.isNotBlank(profileName),"Profile name should be set");
    WsUtils.checkArgument(StringUtils.isNotBlank(profileLang),"Profile language should be set");
    this.profileName = profileName;
    this.profileLang = profileLang;
    this.rules = rules;
  }

  public String getProfileName() {
    return profileName;
  }

  public String getProfileLang() {
    return profileLang;
  }

  public List<ImportedRule> getRules() {
    return rules;
  }
}
