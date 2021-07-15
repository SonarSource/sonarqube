/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as React from 'react';
import { IconProps } from './Icon';
import IssueIcon from './IssueIcon';

export interface Props extends IconProps {
  query: string;
}

export default function IssueTypeIcon({ query, ...iconProps }: Props) {
  let type: T.IssueType;

  switch (query.toLowerCase()) {
    case 'bug':
    case 'bugs':
    case 'new_bugs':
      type = 'BUG';
      break;
    case 'vulnerability':
    case 'vulnerabilities':
    case 'new_vulnerabilities':
      type = 'VULNERABILITY';
      break;
    case 'code_smell':
    case 'code_smells':
    case 'new_code_smells':
      type = 'CODE_SMELL';
      break;
    case 'security_hotspot':
    case 'security_hotspots':
    case 'new_security_hotspots':
      type = 'SECURITY_HOTSPOT';
      break;
    default:
      return null;
  }

  return <IssueIcon type={type} {...iconProps} />;
}
