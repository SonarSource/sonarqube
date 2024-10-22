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
  ButtonIcon,
  ButtonVariety,
  DropdownMenu,
  IconMoreVertical,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { ActionCell, Badge, ContentCell, TableRowInteractive } from '~design-system';
import { isBranch, isMainBranch, isPullRequest } from '~sonar-aligned/helpers/branch-like';
import QualityGateStatus from '../../../app/components/nav/component/branch-like/QualityGateStatus';
import BranchLikeIcon from '../../../components/icon-mappers/BranchLikeIcon';
import DateFromNow from '../../../components/intl/DateFromNow';
import { getBranchLikeDisplayName } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchPurgeSetting from './BranchPurgeSetting';

export interface BranchLikeRowProps {
  branchLike: BranchLike;
  component: Component;
  displayPurgeSetting?: boolean;
  onDelete: () => void;
  onRename: () => void;
  onSetAsMain: () => void;
}

function BranchLikeRow(props: BranchLikeRowProps) {
  const { branchLike, component, displayPurgeSetting } = props;
  const branchLikeDisplayName = getBranchLikeDisplayName(branchLike);

  return (
    <TableRowInteractive>
      <ContentCell>
        <BranchLikeIcon branchLike={branchLike} className="sw-mr-1" />
        <span title={branchLikeDisplayName}>{branchLikeDisplayName}</span>
        <span>
          {isMainBranch(branchLike) && (
            <Badge className="sw-ml-2">{translate('branches.main_branch')}</Badge>
          )}
        </span>
      </ContentCell>
      <ContentCell>
        <QualityGateStatus
          branchLike={branchLike}
          className="sw-flex sw-items-center sw-w-24"
          showStatusText
        />
      </ContentCell>
      <ContentCell>{<DateFromNow date={branchLike.analysisDate} />}</ContentCell>
      {displayPurgeSetting && isBranch(branchLike) && (
        <ContentCell>
          <BranchPurgeSetting branch={branchLike} component={component} />
        </ContentCell>
      )}
      <ActionCell>
        <DropdownMenu.Root
          id={`branch-settings-action-${branchLikeDisplayName}`}
          items={
            <>
              {isBranch(branchLike) && !isMainBranch(branchLike) && (
                <DropdownMenu.ItemButton onClick={props.onSetAsMain}>
                  {translate('project_branch_pull_request.branch.set_main')}
                </DropdownMenu.ItemButton>
              )}

              {isMainBranch(branchLike) ? (
                <DropdownMenu.ItemButton onClick={props.onRename}>
                  {translate('project_branch_pull_request.branch.rename')}
                </DropdownMenu.ItemButton>
              ) : (
                <DropdownMenu.ItemButtonDestructive onClick={props.onDelete}>
                  {translate(
                    isPullRequest(branchLike)
                      ? 'project_branch_pull_request.pull_request.delete'
                      : 'project_branch_pull_request.branch.delete',
                  )}
                </DropdownMenu.ItemButtonDestructive>
              )}
            </>
          }
        >
          <ButtonIcon
            Icon={IconMoreVertical}
            ariaLabel={translateWithParameters(
              'project_branch_pull_request.branch.actions_label',
              branchLikeDisplayName,
            )}
            variety={ButtonVariety.Default}
          />
        </DropdownMenu.Root>
      </ActionCell>
    </TableRowInteractive>
  );
}

export default React.memo(BranchLikeRow);
