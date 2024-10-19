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
import { Badge, FlagErrorIcon, FormField, InputSelect, SelectionCard } from 'design-system';
import * as React from 'react';
import { MenuPlacement, OptionProps, components } from 'react-select';
import Tooltip from '../../../components/controls/Tooltip';
import { NewCodeDefinitionLevels } from '../../../components/new-code-definition/utils';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';

export interface BaselineSettingReferenceBranchProps {
  branchList: BranchOption[];
  className?: string;
  disabled?: boolean;
  inputSelectMenuPlacement?: MenuPlacement;
  onChangeReferenceBranch: (value: string) => void;
  onSelect: (selection: NewCodeDefinitionType) => void;
  referenceBranch: string;
  selected: boolean;
  settingLevel: Exclude<
    NewCodeDefinitionLevels,
    NewCodeDefinitionLevels.NewProject | NewCodeDefinitionLevels.Global
  >;
}

export interface BranchOption {
  isDisabled?: boolean;
  isInvalid?: boolean;
  isMain: boolean;
  label: string;
  value: string;
}

function renderBranchOption(props: OptionProps<BranchOption, false>) {
  const { data: option } = props;

  // For tests and a11y
  props.innerProps.role = 'option';
  props.innerProps['aria-selected'] = props.isSelected;

  return (
    <components.Option {...props}>
      {option.isInvalid ? (
        <Tooltip
          content={translateWithParameters(
            'baseline.reference_branch.does_not_exist',
            option.value,
          )}
        >
          <span>
            {option.value} <FlagErrorIcon className="sw-ml-2" />
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
          {option.isMain && <Badge className="sw-ml-2">{translate('branches.main_branch')}</Badge>}
        </>
      )}
    </components.Option>
  );
}

export default function NewCodeDefinitionSettingReferenceBranch(
  props: Readonly<BaselineSettingReferenceBranchProps>,
) {
  const {
    branchList,
    className,
    disabled,
    referenceBranch,
    selected,
    settingLevel,
    inputSelectMenuPlacement,
  } = props;

  const currentBranch = branchList.find((b) => b.value === referenceBranch) || {
    label: referenceBranch,
    value: referenceBranch,
    isMain: false,
    isInvalid: true,
  };

  return (
    <SelectionCard
      className={className}
      disabled={disabled}
      onClick={() => props.onSelect(NewCodeDefinitionType.ReferenceBranch)}
      selected={selected}
      title={translate('baseline.reference_branch')}
    >
      <>
        <div>
          <p className="sw-mb-3">{translate('baseline.reference_branch.description')}</p>
          <p className="sw-mb-4">{translate('baseline.reference_branch.usecase')}</p>
        </div>
        {selected && (
          <>
            {settingLevel === NewCodeDefinitionLevels.Project && (
              <p>{translate('baseline.reference_branch.description2')}</p>
            )}
            <div className="sw-flex sw-flex-col">
              <MandatoryFieldsExplanation className="sw-mb-2" />

              <FormField
                ariaLabel={translate('baseline.reference_branch.choose')}
                label={translate('baseline.reference_branch.choose')}
                htmlFor="new-code-definition-reference-branch"
                required
              >
                <InputSelect
                  inputId="new-code-definition-reference-branch"
                  className="sw-w-abs-300"
                  size="full"
                  options={branchList}
                  onChange={(option: BranchOption) => props.onChangeReferenceBranch(option.value)}
                  value={currentBranch}
                  components={{
                    Option: renderBranchOption,
                  }}
                  menuPlacement={inputSelectMenuPlacement}
                />
              </FormField>
            </div>
          </>
        )}
      </>
    </SelectionCard>
  );
}
