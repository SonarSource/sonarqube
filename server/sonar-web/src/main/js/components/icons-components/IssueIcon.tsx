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
import * as React from 'react';
import BugIcon from '../icons-components/BugIcon';
import VulnerabilityIcon from '../icons-components/VulnerabilityIcon';
import CodeSmellIcon from '../icons-components/CodeSmellIcon';
import SecurityHotspotIcon from '../icons-components/SecurityHotspotIcon';

interface Props {
  className?: string;
  type: T.IssueType;
  size?: number;
}

export default function IssueIcon({ className, type, size }: Props) {
  switch (type) {
    case 'BUG':
      return <BugIcon className={className} size={size} />;
    case 'VULNERABILITY':
      return <VulnerabilityIcon className={className} size={size} />;
    case 'CODE_SMELL':
      return <CodeSmellIcon className={className} size={size} />;
    case 'SECURITY_HOTSPOT':
      return <SecurityHotspotIcon className={className} size={size} />;
    default:
      return null;
  }
}
