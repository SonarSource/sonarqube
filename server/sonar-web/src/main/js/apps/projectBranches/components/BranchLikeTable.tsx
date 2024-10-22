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
import { ActionCell, ContentCell, HelperHintIcon, Table, TableRow } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { getBranchLikeKey } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchLikeRow from './BranchLikeRow';

const COLUMN_WIDTHS_WITH_PURGE_SETTING = ['auto', '10%', '15%', '15%', '5%'];
const COLUMN_WIDTHS_WITHOUT_PURGE_SETTING = ['auto', '10%', '15%', '5%'];

export interface BranchLikeTableProps {
  branchLikes: BranchLike[];
  component: Component;
  displayPurgeSetting?: boolean;
  onDelete: (branchLike: BranchLike) => void;
  onRename: (branchLike: BranchLike) => void;
  onSetAsMain: (branchLike: BranchLike) => void;
  title: string;
}

function BranchLikeTable(props: BranchLikeTableProps) {
  const { branchLikes, component, displayPurgeSetting, title } = props;

  const header = (
    <TableRow>
      <ContentCell>{title}</ContentCell>
      <ContentCell>{translate('status')}</ContentCell>
      <ContentCell>{translate('project_branch_pull_request.last_analysis_date')}</ContentCell>
      {displayPurgeSetting && (
        <ContentCell>
          <div className="sw-flex sw-items-center">
            <span>
              {translate('project_branch_pull_request.branch.auto_deletion.keep_when_inactive')}
            </span>
            <HelpTooltip
              className="sw-ml-1"
              overlay={translate(
                'project_branch_pull_request.branch.auto_deletion.keep_when_inactive.tooltip',
              )}
            >
              <HelperHintIcon />
            </HelpTooltip>
          </div>
        </ContentCell>
      )}

      <ActionCell>{translate('actions')}</ActionCell>
    </TableRow>
  );

  return (
    <div className="sw-mt-6">
      <Table
        columnCount={
          displayPurgeSetting
            ? COLUMN_WIDTHS_WITH_PURGE_SETTING.length
            : COLUMN_WIDTHS_WITHOUT_PURGE_SETTING.length
        }
        columnWidths={
          displayPurgeSetting
            ? COLUMN_WIDTHS_WITH_PURGE_SETTING
            : COLUMN_WIDTHS_WITHOUT_PURGE_SETTING
        }
        header={header}
      >
        {branchLikes.map((branchLike) => (
          <BranchLikeRow
            branchLike={branchLike}
            component={component}
            displayPurgeSetting={displayPurgeSetting}
            key={getBranchLikeKey(branchLike)}
            onDelete={() => props.onDelete(branchLike)}
            onRename={() => props.onRename(branchLike)}
            onSetAsMain={() => props.onSetAsMain(branchLike)}
          />
        ))}
      </Table>
    </div>
  );
}

export default React.memo(BranchLikeTable);
