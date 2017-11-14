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
package org.sonarqube.ws.client.editions;

/*
 * THIS FILE HAS BEEN AUTOMATICALLY GENERATED
 */

import java.util.List;

/**
 * Apply changes to SonarQube to match the specified license. Clear error message of previous automatic install of an edition, if there is any. Require 'Administer System' permission.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/apply_license">Further information about this action online (including a response example)</a>
 * @since 6.7
 */
public class ApplyLicenseRequest {

  private String license;

  /**
   * the license
   *
   * This is a mandatory parameter.
   */
  public ApplyLicenseRequest setLicense(String license) {
    this.license = license;
    return this;
  }

  public String getLicense() {
    return license;
  }
}
