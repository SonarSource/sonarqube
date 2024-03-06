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
import {
  BugIcon,
  CodeSmellIcon,
  IconProps,
  SecurityHotspotIcon,
  VulnerabilityIcon,
} from 'design-system';
import React from 'react';
import { IssueType } from '../../types/issues';

export interface Props extends IconProps {
  type: string | IssueType;
}

export default function IssueTypeIcon({ type, ...iconProps }: Props) {
  switch (type.toLowerCase()) {
    case IssueType.Bug.toLowerCase():
    case 'bugs':
    case 'new_bugs':
    case IssueType.Bug:
      return <BugIcon {...iconProps} />;
    case IssueType.Vulnerability.toLowerCase():
    case 'vulnerabilities':
    case 'new_vulnerabilities':
    case IssueType.Vulnerability:
      return <VulnerabilityIcon {...iconProps} />;
    case IssueType.CodeSmell.toLowerCase():
    case 'code_smells':
    case 'new_code_smells':
    case IssueType.CodeSmell:
      return <CodeSmellIcon {...iconProps} />;
    case IssueType.SecurityHotspot.toLowerCase():
    case 'security_hotspots':
    case 'new_security_hotspots':
    case IssueType.SecurityHotspot:
      return <SecurityHotspotIcon {...iconProps} />;
    default:
      return null;
  }
}
