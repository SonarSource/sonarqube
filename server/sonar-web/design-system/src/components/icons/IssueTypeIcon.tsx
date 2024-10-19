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
import { IssueType } from '../../types';
import { BugIcon } from './BugIcon';
import { CodeSmellIcon } from './CodeSmellIcon';
import { IconProps } from './Icon';
import { SecurityFindingIcon } from './SecurityFindingIcon';
import { VulnerabilityIcon } from './VulnerabilityIcon';

export interface Props extends IconProps {
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  type: string | IssueType;
}

export enum IssueTypeEnum {
  CODE_SMELL = 'CODE_SMELL',
  VULNERABILITY = 'VULNERABILITY',
  BUG = 'BUG',
  SECURITY_HOTSPOT = 'SECURITY_HOTSPOT',
}

export function IssueTypeIcon({ type, ...iconProps }: Props) {
  switch (type.toLowerCase()) {
    case IssueTypeEnum.BUG.toLowerCase():
    case 'bugs':
    case 'new_bugs':
    case IssueTypeEnum.BUG:
      return <BugIcon {...iconProps} />;
    case IssueTypeEnum.VULNERABILITY.toLowerCase():
    case 'vulnerabilities':
    case 'new_vulnerabilities':
    case IssueTypeEnum.VULNERABILITY:
      return <VulnerabilityIcon {...iconProps} />;
    case IssueTypeEnum.CODE_SMELL.toLowerCase():
    case 'code_smells':
    case 'new_code_smells':
    case IssueTypeEnum.CODE_SMELL:
      return <CodeSmellIcon {...iconProps} />;
    case IssueTypeEnum.SECURITY_HOTSPOT.toLowerCase():
    case 'security_hotspots':
    case 'new_security_hotspots':
    case IssueTypeEnum.SECURITY_HOTSPOT:
      return <SecurityFindingIcon {...iconProps} />;
    default:
      return null;
  }
}
