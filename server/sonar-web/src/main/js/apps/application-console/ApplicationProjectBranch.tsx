/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import BranchIcon from '../../components/icons/BranchIcon';
import { translate } from '../../helpers/l10n';
import { Application } from '../../types/application';
import BranchRowActions from './BranchRowActions';
import { ApplicationBranch } from './utils';

export interface ApplicationProjectBranchProps {
  application: Application;
  branch: ApplicationBranch;
  onUpdateBranches: (branches: Array<ApplicationBranch>) => void;
}

export default function ApplicationProjectBranch(props: ApplicationProjectBranchProps) {
  const { application, branch } = props;
  return (
    <tr>
      <td>
        <BranchIcon className="little-spacer-right" />
        {branch.name}
        {branch.isMain && (
          <span className="badge spacer-left">
            {translate('application_console.branches.main_branch')}
          </span>
        )}
      </td>
      <td className="thin nowrap">
        {!branch.isMain && (
          <BranchRowActions
            application={application}
            branch={branch}
            onUpdateBranches={props.onUpdateBranches}
          />
        )}
      </td>
    </tr>
  );
}
