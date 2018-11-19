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
import BranchStatus from '../../../../components/common/BranchStatus';
import { Branch, Component } from '../../../types';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Tooltip from '../../../../components/controls/Tooltip';
import { isShortLivingBranch } from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';

interface Props {
  branch?: Branch;
  component: Component;
}

export default function ComponentNavMeta(props: Props) {
  const metaList = [];
  const shortBranch = props.branch && isShortLivingBranch(props.branch);

  if (props.component.analysisDate) {
    metaList.push(
      <li key="analysisDate">
        <DateTimeFormatter date={props.component.analysisDate} />
      </li>
    );
  }

  if (props.component.version && !shortBranch) {
    metaList.push(
      <li key="version">
        <Tooltip
          overlay={`${translate('version')} ${props.component.version}`}
          mouseEnterDelay={0.5}>
          <span className="text-limited">
            {translate('version')} {props.component.version}
          </span>
        </Tooltip>
      </li>
    );
  }

  return (
    <div className="navbar-context-meta">
      <ul className="list-inline">{metaList}</ul>
      {shortBranch && (
        <div className="navbar-context-meta-branch">
          <BranchStatus branch={props.branch!} />
        </div>
      )}
    </div>
  );
}
