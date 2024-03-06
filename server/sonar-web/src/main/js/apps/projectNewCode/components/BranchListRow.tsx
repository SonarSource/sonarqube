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
  ActionCell,
  ActionsDropdown,
  Badge,
  ContentCell,
  FlagWarningIcon,
  InteractiveIcon,
  ItemButton,
  PencilIcon,
  TableRowInteractive,
} from 'design-system';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import BranchLikeIcon from '../../../components/icon-mappers/BranchLikeIcon';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isNewCodeDefinitionCompliant } from '../../../helpers/new-code-definition';
import { BranchWithNewCodePeriod } from '../../../types/branch-like';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../../types/new-code-definition';

export interface BranchListRowProps {
  branch: BranchWithNewCodePeriod;
  existingBranches: Array<string>;
  inheritedSetting: NewCodeDefinition;
  onOpenEditModal: (branch: BranchWithNewCodePeriod) => void;
  onResetToDefault: (branchName: string) => void;
}

function renderNewCodePeriodSetting(newCodePeriod: NewCodeDefinition) {
  switch (newCodePeriod.type) {
    case NewCodeDefinitionType.SpecificAnalysis:
      return (
        <>
          {`${translate('baseline.specific_analysis')}: `}
          {newCodePeriod.effectiveValue ? (
            <DateTimeFormatter date={newCodePeriod.effectiveValue} />
          ) : (
            '?'
          )}
        </>
      );
    case NewCodeDefinitionType.NumberOfDays:
      return `${translate('new_code_definition.number_days')}: ${newCodePeriod.value}`;
    case NewCodeDefinitionType.PreviousVersion:
      return translate('new_code_definition.previous_version');
    case NewCodeDefinitionType.ReferenceBranch:
      return `${translate('baseline.reference_branch')}: ${newCodePeriod.value}`;
    default:
      return newCodePeriod.type;
  }
}

function branchInheritsItselfAsReference(
  branch: BranchWithNewCodePeriod,
  inheritedSetting: NewCodeDefinition,
) {
  return (
    !branch.newCodePeriod &&
    inheritedSetting.type === NewCodeDefinitionType.ReferenceBranch &&
    branch.name === inheritedSetting.value
  );
}

function referenceBranchDoesNotExist(
  branch: BranchWithNewCodePeriod,
  existingBranches: Array<string>,
) {
  return (
    branch.newCodePeriod?.value &&
    branch.newCodePeriod.type === NewCodeDefinitionType.ReferenceBranch &&
    !existingBranches.includes(branch.newCodePeriod.value)
  );
}

export default function BranchListRow(props: BranchListRowProps) {
  const { branch, existingBranches, inheritedSetting } = props;

  let settingWarning: string | undefined;
  if (branchInheritsItselfAsReference(branch, inheritedSetting)) {
    settingWarning = translateWithParameters(
      'baseline.reference_branch.invalid_branch_setting',
      branch.name,
    );
  } else if (referenceBranchDoesNotExist(branch, existingBranches)) {
    settingWarning = translateWithParameters(
      'baseline.reference_branch.does_not_exist',
      branch.newCodePeriod?.value ?? '',
    );
  }

  const isCompliant = isNewCodeDefinitionCompliant(inheritedSetting);

  return (
    <TableRowInteractive>
      <ContentCell>
        <BranchLikeIcon branchLike={branch} className="sw-mr-1" />
        {branch.name}
        {branch.isMain && <Badge className="sw-ml-1">{translate('branches.main_branch')}</Badge>}
      </ContentCell>
      <ContentCell>
        <Tooltip overlay={settingWarning}>
          <span>
            {settingWarning !== undefined && <FlagWarningIcon className="sw-mr-1" />}
            {branch.newCodePeriod
              ? renderNewCodePeriodSetting(branch.newCodePeriod)
              : translate('branch_list.default_setting')}
          </span>
        </Tooltip>
      </ContentCell>
      <ActionCell>
        {!branch.newCodePeriod && (
          <InteractiveIcon
            Icon={PencilIcon}
            aria-label={translateWithParameters('branch_list.edit_for_x', branch.name)}
            onClick={() => props.onOpenEditModal(branch)}
            className="sw-mr-2"
            size="small"
          />
        )}
        {branch.newCodePeriod && (
          <ActionsDropdown
            allowResizing
            id={`new-code-action-${branch.name}`}
            ariaLabel={translateWithParameters('branch_list.show_actions_for_x', branch.name)}
          >
            <Tooltip
              overlay={
                isCompliant ? null : translate('project_baseline.compliance.warning.title.project')
              }
            >
              <ItemButton
                disabled={!isCompliant}
                onClick={() => props.onResetToDefault(branch.name)}
              >
                {translate('reset_to_default')}
              </ItemButton>
            </Tooltip>
            <ItemButton onClick={() => props.onOpenEditModal(branch)}>
              {translate('edit')}
            </ItemButton>
          </ActionsDropdown>
        )}
      </ActionCell>
    </TableRowInteractive>
  );
}
