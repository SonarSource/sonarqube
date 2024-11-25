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
  Button,
  ButtonGroup,
  ButtonVariety,
  Label,
  RadioButtonGroup,
} from '@sonarsource/echoes-react';
import { noop } from 'lodash';
import * as React from 'react';
import { FlagMessage } from '~design-system';
import GlobalNewCodeDefinitionDescription from '../../../components/new-code-definition/GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionDaysOption from '../../../components/new-code-definition/NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from '../../../components/new-code-definition/NewCodeDefinitionPreviousVersionOption';
import { NewCodeDefinitionLevels } from '../../../components/new-code-definition/utils';
import { translate } from '../../../helpers/l10n';
import { Branch } from '../../../types/branch-like';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../../types/new-code-definition';
import { validateSetting } from '../utils';
import NewCodeDefinitionSettingAnalysis from './NewCodeDefinitionSettingAnalysis';
import NewCodeDefinitionSettingReferenceBranch from './NewCodeDefinitionSettingReferenceBranch';

export interface ProjectBaselineSelectorProps {
  analysis?: string;
  branch?: Branch;
  branchList: Branch[];
  branchesEnabled?: boolean;
  component: string;
  days: string;
  globalNewCodeDefinition: NewCodeDefinition;
  isChanged: boolean;
  newCodeDefinitionType?: NewCodeDefinitionType;
  newCodeDefinitionValue?: string;
  onCancel: () => void;
  onSelectDays: (value: string) => void;
  onSelectReferenceBranch: (value: string) => void;
  onSelectSetting: (value: NewCodeDefinitionType) => void;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  onToggleSpecificSetting: (selection: boolean) => void;
  overrideGlobalNewCodeDefinition: boolean;
  previousNonCompliantValue?: string;
  projectNcdUpdatedAt?: number;
  referenceBranch?: string;
  saving: boolean;
  selectedNewCodeDefinitionType?: NewCodeDefinitionType;
}

function branchToOption(b: Branch) {
  return { label: b.name, value: b.name, isMain: b.isMain };
}

export default function ProjectNewCodeDefinitionSelector(
  props: Readonly<ProjectBaselineSelectorProps>,
) {
  const {
    analysis,
    branch,
    branchesEnabled,
    branchList,
    component,
    days,
    globalNewCodeDefinition,
    isChanged,
    newCodeDefinitionType,
    newCodeDefinitionValue,
    overrideGlobalNewCodeDefinition,
    previousNonCompliantValue,
    projectNcdUpdatedAt,
    referenceBranch,
    saving,
    selectedNewCodeDefinitionType,
  } = props;

  const isValid = validateSetting({
    numberOfDays: days,
    overrideGlobalNewCodeDefinition,
    referenceBranch,
    selectedNewCodeDefinitionType,
  });

  if (branch === undefined) {
    return null;
  }

  return (
    <form className="it__project-baseline-selector " onSubmit={props.onSubmit}>
      <fieldset>
        <legend className="sw-mb-4">
          <Label>{translate('project_baseline.page.question')}</Label>
        </legend>
        <RadioButtonGroup
          id="new-code-baseline-radiogroup"
          onChange={(value: 'general' | 'specific') =>
            props.onToggleSpecificSetting(value === 'specific')
          }
          value={overrideGlobalNewCodeDefinition ? 'specific' : 'general'}
          options={[
            {
              value: 'general',
              label: translate('project_baseline.global_setting'),
              helpText: (
                <GlobalNewCodeDefinitionDescription
                  className="sw-mt-2 sw-mb-6"
                  globalNcd={globalNewCodeDefinition}
                />
              ),
            },
            { value: 'specific', label: translate('project_baseline.specific_setting') },
          ]}
        />

        <div className="sw-flex sw-flex-col sw-gap-4" role="radiogroup">
          <NewCodeDefinitionPreviousVersionOption
            disabled={!overrideGlobalNewCodeDefinition}
            onSelect={props.onSelectSetting}
            selected={
              overrideGlobalNewCodeDefinition &&
              selectedNewCodeDefinitionType === NewCodeDefinitionType.PreviousVersion
            }
          />

          <NewCodeDefinitionDaysOption
            currentDaysValue={
              newCodeDefinitionType === NewCodeDefinitionType.NumberOfDays
                ? newCodeDefinitionValue
                : undefined
            }
            days={days}
            disabled={!overrideGlobalNewCodeDefinition}
            isValid={isValid}
            onChangeDays={props.onSelectDays}
            onSelect={props.onSelectSetting}
            previousNonCompliantValue={previousNonCompliantValue}
            selected={
              overrideGlobalNewCodeDefinition &&
              selectedNewCodeDefinitionType === NewCodeDefinitionType.NumberOfDays
            }
            settingLevel={NewCodeDefinitionLevels.Project}
            updatedAt={projectNcdUpdatedAt}
          />

          {branchesEnabled && (
            <NewCodeDefinitionSettingReferenceBranch
              branchList={branchList.map(branchToOption)}
              disabled={!overrideGlobalNewCodeDefinition}
              onChangeReferenceBranch={props.onSelectReferenceBranch}
              onSelect={props.onSelectSetting}
              referenceBranch={referenceBranch ?? ''}
              selected={
                overrideGlobalNewCodeDefinition &&
                selectedNewCodeDefinitionType === NewCodeDefinitionType.ReferenceBranch
              }
              settingLevel={NewCodeDefinitionLevels.Project}
            />
          )}

          {!branchesEnabled && newCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis && (
            <NewCodeDefinitionSettingAnalysis
              analysis={analysis ?? ''}
              branch={branch.name}
              component={component}
              onSelect={noop}
              selected={
                overrideGlobalNewCodeDefinition &&
                selectedNewCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis
              }
            />
          )}
        </div>
      </fieldset>

      <div className="sw-mt-4">
        <output>
          {isChanged && (
            <FlagMessage variant="info" className="sw-mb-4">
              {translate('baseline.next_analysis_notice')}
            </FlagMessage>
          )}
        </output>

        <ButtonGroup className="sw-flex">
          <Button
            type="submit"
            isLoading={saving}
            isDisabled={!isValid || !isChanged || saving}
            variety={ButtonVariety.Primary}
          >
            {translate('save')}
          </Button>

          <Button isDisabled={saving || !isChanged} onClick={props.onCancel}>
            {translate('cancel')}
          </Button>
        </ButtonGroup>
      </div>
    </form>
  );
}
