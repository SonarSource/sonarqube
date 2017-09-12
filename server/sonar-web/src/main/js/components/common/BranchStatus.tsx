/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import * as classNames from 'classnames';
import { Branch } from '../../app/types';
import Level from '../ui/Level';
import BugIcon from '../icons-components/BugIcon';
import CodeSmellIcon from '../icons-components/CodeSmellIcon';
import VulnerabilityIcon from '../icons-components/VulnerabilityIcon';
import { isShortLivingBranch } from '../../helpers/branches';
import './BranchStatus.css';

interface Props {
  branch: Branch;
  concise?: boolean;
}

export default function BranchStatus({ branch, concise = false }: Props) {
  if (isShortLivingBranch(branch)) {
    if (!branch.status) {
      return null;
    }

    const totalIssues =
      branch.status.bugs + branch.status.vulnerabilities + branch.status.codeSmells;

    return (
      <ul className="list-inline branch-status">
        <li>
          <i
            className={classNames('branch-status-indicator', {
              'is-failed': totalIssues > 0,
              'is-passed': totalIssues === 0
            })}
          />
        </li>
        {concise &&
          <li>
            {totalIssues}
          </li>}
        {!concise &&
          <li>
            {branch.status.bugs}
            <BugIcon className="little-spacer-left" />
          </li>}
        {!concise &&
          <li>
            {branch.status.vulnerabilities}
            <VulnerabilityIcon className="little-spacer-left" />
          </li>}
        {!concise &&
          <li>
            {branch.status.codeSmells}
            <CodeSmellIcon className="little-spacer-left" />
          </li>}
      </ul>
    );
  } else {
    if (!branch.status) {
      return null;
    }

    return <Level level={branch.status.qualityGateStatus} small={true} />;
  }
}
