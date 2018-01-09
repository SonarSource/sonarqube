/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

interface Props {
  className?: string;
  query: string;
}

export default function IssueTypeIcon(props: Props) {
  let icon;

  switch (props.query.toLowerCase()) {
    case 'bug':
    case 'bugs':
    case 'new_bugs':
      icon = <BugIcon />;
      break;
    case 'vulnerability':
    case 'vulnerabilities':
    case 'new_vulnerabilities':
      icon = <VulnerabilityIcon />;
      break;
    case 'code_smell':
    case 'code_smells':
    case 'new_code_smells':
      icon = <CodeSmellIcon />;
      break;
  }

  if (!icon) {
    return null;
  }

  return props.className ? <span className={props.className}>{icon}</span> : icon;
}
