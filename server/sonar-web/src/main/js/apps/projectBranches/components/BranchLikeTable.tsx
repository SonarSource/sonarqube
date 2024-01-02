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
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { getBranchLikeKey } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchLikeRow from './BranchLikeRow';

export interface BranchLikeTableProps {
  branchLikes: BranchLike[];
  component: Component;
  displayPurgeSetting?: boolean;
  onDelete: (branchLike: BranchLike) => void;
  onRename: (branchLike: BranchLike) => void;
  onUpdatePurgeSetting: () => void;
  title: string;
}

export function BranchLikeTable(props: BranchLikeTableProps) {
  const { branchLikes, component, displayPurgeSetting, title } = props;

  return (
    <div className="boxed-group boxed-group-inner">
      <table className="data zebra zebra-hover fixed">
        <thead>
          <tr>
            <th className="nowrap">{title}</th>
            <th className="nowrap" style={{ width: '80px' }}>
              {translate('status')}
            </th>
            <th className="nowrap" style={{ width: '140px' }}>
              {translate('project_branch_pull_request.last_analysis_date')}
            </th>
            {displayPurgeSetting && (
              <th className="nowrap" style={{ width: '150px' }}>
                <div className="display-flex-center">
                  <span>
                    {translate(
                      'project_branch_pull_request.branch.auto_deletion.keep_when_inactive'
                    )}
                  </span>
                  <HelpTooltip
                    className="little-spacer-left"
                    overlay={translate(
                      'project_branch_pull_request.branch.auto_deletion.keep_when_inactive.tooltip'
                    )}
                  />
                </div>
              </th>
            )}
            <th className="nowrap" style={{ width: '50px' }}>
              {translate('actions')}
            </th>
          </tr>
        </thead>
        <tbody>
          {branchLikes.map((branchLike) => (
            <BranchLikeRow
              branchLike={branchLike}
              component={component}
              displayPurgeSetting={displayPurgeSetting}
              key={getBranchLikeKey(branchLike)}
              onDelete={() => props.onDelete(branchLike)}
              onRename={() => props.onRename(branchLike)}
              onUpdatePurgeSetting={props.onUpdatePurgeSetting}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default React.memo(BranchLikeTable);
