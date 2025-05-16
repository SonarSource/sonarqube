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

export const severityOptions = [
  { value: '', label: '--None--' },
  { value: 'High', label: 'High' },
  { value: 'Medium', label: 'Medium' },
  { value: 'Low', label: 'Low' },
  { value: 'Critical', label: 'Critical' }
];

export const productOptions = [
  { value: '', label: '--None--' },
  { value: 'CodeScan', label: 'CodeScan' },
  { value: 'MuleSoft', label: 'MuleSoft' },
  { value: 'Other', label: 'Other' }
];

export const caseRecordTypeOptions = [
  { value: '', label: '--None--' },
  { value: '012cs000001CFlt', label: 'Non-Support' },
  { value: '012cs000001CFkH', label: 'Support' }
];

export const categoryOptions = [
  { value: '', label: '--None--' },
  { value: 'App Issue', label: 'App Issue' },
  { value: 'Feature Suggestion', label: 'Feature Suggestion' },
  { value: 'Others', label: 'Others' },
  { value: 'Performance', label: 'Performance' },
  { value: 'Question', label: 'Question' }
];

export const classificationOptions = [
  { value: '', label: '--None--' },
  { value: 'CodeScan - Admin', label: 'CodeScan - Admin' },
  { value: 'CodeScan - Members', label: 'CodeScan - Members' },
  { value: 'CodeScan - Issues', label: 'CodeScan - Issues' },
  { value: 'Codescan - Plugins', label: 'Codescan - Plugins' },
  { value: 'Codescan - Projects', label: 'Codescan - Projects' },
  { value: 'Codescan - Quality Gates', label: 'Codescan - Quality Gates' },
  { value: 'Codescan - Quality Profiles', label: 'Codescan - Quality Profiles' },
  { value: 'Codescan - Reports', label: 'Codescan - Reports' },
  { value: 'Codescan - Rules', label: 'Codescan - Rules' },
  { value: 'Other', label: 'Other' }
];

export const hostingTypeOptions = [
  { value: '', label: '--None--' },
  { value: 'Shared Cloud', label: 'Shared Cloud' },
  { value: 'Dedicated', label: 'Dedicated' },
  { value: 'On Premise', label: 'On Premise' }
];

export const versionOptions = [
  { value: '', label: '--None--' },
  { value: '25.1.0', label: '25.1.0' }
];

export const pluginTypeOptions = [
  { value: '', label: '--None--' },
  { value: 'VS Code', label: 'VS Code' },
  { value: 'IntelliJ', label: 'IntelliJ' },
  { value: 'SFDX', label: 'SFDX' },
  { value: 'Azure', label: 'Azure' },
  { value: 'Github Action Integration', label: 'Github Action Integration' },
  { value: 'Github SARIF Reports', label: 'Github SARIF Reports' },
  { value: 'ARM', label: 'ARM' },
  { value: 'CodeScan', label: 'CodeScan' },
  { value: 'SonarQube', label: 'SonarQube' }
];