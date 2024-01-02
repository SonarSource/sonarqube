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
import { components, OptionProps } from 'react-select';
import RadioCard from '../../../components/controls/RadioCard';
import Select from '../../../components/controls/Select';
import Tooltip from '../../../components/controls/Tooltip';
import AlertErrorIcon from '../../../components/icons/AlertErrorIcon';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { NewCodePeriodSettingType } from '../../../types/types';

export interface BaselineSettingReferenceBranchProps {
  branchList: BranchOption[];
  className?: string;
  disabled?: boolean;
  onChangeReferenceBranch: (value: string) => void;
  onSelect: (selection: NewCodePeriodSettingType) => void;
  referenceBranch: string;
  selected: boolean;
  settingLevel: 'project' | 'branch';
}

export interface BranchOption {
  isDisabled?: boolean;
  isInvalid?: boolean;
  isMain: boolean;
  label: string;
  value: string;
}

/* Export for testing */
export function renderBranchOption(props: OptionProps<BranchOption, false>) {
  const { data: option } = props;

  return (
    <components.Option {...props}>
      {option.isInvalid ? (
        <Tooltip
          overlay={translateWithParameters(
            'baseline.reference_branch.does_not_exist',
            option.value
          )}
        >
          <span>
            {option.value} <AlertErrorIcon />
          </span>
        </Tooltip>
      ) : (
        <>
          <span
            title={
              option.isDisabled
                ? translate('baseline.reference_branch.cannot_be_itself')
                : undefined
            }
          >
            {option.value}
          </span>
          {option.isMain && (
            <div className="badge spacer-left">{translate('branches.main_branch')}</div>
          )}
        </>
      )}
    </components.Option>
  );
}

export default function BaselineSettingReferenceBranch(props: BaselineSettingReferenceBranchProps) {
  const { branchList, className, disabled, referenceBranch, selected, settingLevel } = props;

  const currentBranch = branchList.find((b) => b.value === referenceBranch) || {
    label: referenceBranch,
    value: referenceBranch,
    isMain: false,
    isInvalid: true,
  };

  return (
    <RadioCard
      className={className}
      disabled={disabled}
      onClick={() => props.onSelect('REFERENCE_BRANCH')}
      selected={selected}
      title={translate('baseline.reference_branch')}
    >
      <>
        <p>{translate('baseline.reference_branch.description')}</p>
        {selected && (
          <>
            {settingLevel === 'project' && (
              <p className="spacer-top">{translate('baseline.reference_branch.description2')}</p>
            )}
            <div className="big-spacer-top display-flex-column">
              <MandatoryFieldsExplanation className="spacer-bottom" />
              <label className="text-middle" htmlFor="reference_branch">
                <strong>{translate('baseline.reference_branch.choose')}</strong>
                <MandatoryFieldMarker />
              </label>
              <Select
                className="little-spacer-top spacer-bottom"
                options={branchList}
                onChange={(option: BranchOption) => props.onChangeReferenceBranch(option.value)}
                value={currentBranch}
                components={{
                  Option: renderBranchOption,
                }}
              />
            </div>
          </>
        )}
      </>
    </RadioCard>
  );
}
