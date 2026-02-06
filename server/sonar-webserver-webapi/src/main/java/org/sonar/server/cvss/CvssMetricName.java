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
package org.sonar.server.cvss;

/**
 * CVSS v3.1 metric names. The enum values are used in JSON and mapped back when loading the CVSS metadata.
 */
public enum CvssMetricName {

    ATTACK_VECTOR("Attack Vector (AV)"),
    ATTACK_COMPLEXITY("Attack Complexity (AC)"),
    PRIVILEGES_REQUIRED("Privileges Required (PR)"),
    USER_INTERACTION("User Interaction (UI)"),
    SCOPE("Scope (S)"),
    CONFIDENTIALITY("Confidentiality (C)"),
    INTEGRITY("Integrity (I)"),
    AVAILABILITY("Availability (A)"),

    EXPLOIT_CODE_MATURITY("Exploit Code Maturity (E)"),
    REMEDIATION_LEVEL("Remediation Level (RL)"),
    REPORT_CONFIDENCE("Report Confidence (RC)"),

    MODIFIED_ATTACK_VECTOR("Modified Attack Vector (MAV)"),
    MODIFIED_ATTACK_COMPLEXITY("Modified Attack Complexity (MAC)"),
    MODIFIED_PRIVILEGES_REQUIRED("Modified Privileges Required (MPR)"),
    MODIFIED_USER_INTERACTION("Modified User Interaction (MUI)"),
    MODIFIED_SCOPE("Modified Scope (MS)"),
    MODIFIED_CONFIDENTIALITY("Modified Confidentiality (MC)"),
    MODIFIED_INTEGRITY("Modified Integrity (MI)"),
    MODIFIED_AVAILABILITY("Modified Availability (MA)"),
    CONFIDENTIALITY_REQUIREMENT("Confidentiality Requirement (CR)"),
    INTEGRITY_REQUIREMENT("Integrity Requirement (IR)"),
    AVAILABILITY_REQUIREMENT("Availability Requirement (AR)");


    private final String displayName;

    CvssMetricName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


