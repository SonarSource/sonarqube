/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.core.config;

public class SecurityStandardParameters {

  public static final String PARAM_CWE = "cwe";
  public static final String PARAM_CWE_TOP_25 = "cweTop25";
  public static final String PARAM_OWASP_ASVS = "owaspAsvs";
  public static final String PARAM_OWASP_ASVS_40 = "owaspAsvs-4.0";
  public static final String PARAM_OWASP_ASVS_LEVEL = "owaspAsvsLevel";
  public static final String PARAM_OWASP_LLM_TOP_10 = "owaspLlmTop10";
  public static final String PARAM_OWASP_MASVS = "owaspMasvs-v2";
  public static final String PARAM_OWASP_MOBILE_TOP_10 = "owaspMobileTop10";
  public static final String PARAM_OWASP_MOBILE_TOP_10_2024 = "owaspMobileTop10-2024";
  public static final String PARAM_OWASP_TOP_10 = "owaspTop10";
  public static final String PARAM_OWASP_TOP_10_2021 = "owaspTop10-2021";
  public static final String PARAM_PCI_DSS = "pciDss";
  public static final String PARAM_PCI_DSS_32 = "pciDss-3.2";
  public static final String PARAM_PCI_DSS_40 = "pciDss-4.0";
  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated(since = "10.0", forRemoval = true)
  public static final String PARAM_SANS_TOP_25 = "sansTop25";
  public static final String PARAM_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  public static final String PARAM_STIG = "stig";
  public static final String PARAM_STIG_ASD_V5R3 = "stig-ASD_V5R3";
  public static final String PARAM_CASA = "casa";

  private SecurityStandardParameters() {
    // class cannot be instantiated
  }
}
