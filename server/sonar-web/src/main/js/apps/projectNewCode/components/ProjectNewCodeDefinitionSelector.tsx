/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import { RadioButton } from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import GlobalNewCodeDefinitionDescription from '../../../components/new-code-definition/GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionAnalysisWarning from '../../../components/new-code-definition/NewCodeDefinitionAnalysisWarning';
import NewCodeDefinitionDaysOption from '../../../components/new-code-definition/NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from '../../../components/new-code-definition/NewCodeDefinitionPreviousVersionOption';
import { NewCodeDefinitionLevels } from '../../../components/new-code-definition/utils';
import { Alert } from '../../../components/ui/Alert';
import Spinner from '../../../components/ui/Spinner';
import { translate } from '../../../helpers/l10n';
import { Branch } from '../../../types/branch-like';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../../types/new-code-definition';
import { validateSetting } from '../utils';
import BranchAnalysisList from './BranchAnalysisList';
import NewCodeDefinitionSettingAnalysis from './NewCodeDefinitionSettingAnalysis';
import NewCodeDefinitionSettingReferenceBranch from './NewCodeDefinitionSettingReferenceBranch';

export interface ProjectBaselineSelectorProps {
  analysis?: string;
  branch?: Branch;
  branchList: Branch[];
  branchesEnabled?: boolean;
  component: string;
  newCodeDefinitionType?: NewCodeDefinitionType;
  newCodeDefinitionValue?: string;
  previousNonCompliantValue?: string;
  projectNcdUpdatedAt?: number;
  days: string;
  globalNewCodeDefinition: NewCodeDefinition;
  isChanged: boolean;
  onCancel: () => void;
  onSelectDays: (value: string) => void;
  onSelectReferenceBranch: (value: string) => void;
  onSelectSetting: (value: NewCodeDefinitionType) => void;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  onToggleSpecificSetting: (selection: boolean) => void;
  referenceBranch?: string;
  saving: boolean;
  selectedNewCodeDefinitionType?: NewCodeDefinitionType;
  overrideGlobalNewCodeDefinition: boolean;
}

function branchToOption(b: Branch) {
  return { label: b.name, value: b.name, isMain: b.isMain };
}

export default function ProjectNewCodeDefinitionSelector(props: ProjectBaselineSelectorProps) {
  const {
    analysis,
    branch,
    branchList,
    branchesEnabled,
    component,
    newCodeDefinitionType,
    newCodeDefinitionValue,
    previousNonCompliantValue,
    projectNcdUpdatedAt,
    days,
    globalNewCodeDefinition,
    isChanged,
    overrideGlobalNewCodeDefinition,
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
    <form className="project-baseline-selector" onSubmit={props.onSubmit}>
      <div className="big-spacer-top spacer-bottom" role="radiogroup">
        <RadioButton
          checked={!overrideGlobalNewCodeDefinition}
          className="big-spacer-bottom"
          onCheck={() => props.onToggleSpecificSetting(false)}
          value="general"
        >
          <span>{translate('project_baseline.global_setting')}</span>
        </RadioButton>

        <div className="sw-ml-4">
          <GlobalNewCodeDefinitionDescription globalNcd={globalNewCodeDefinition} />
        </div>

        <RadioButton
          checked={overrideGlobalNewCodeDefinition}
          className="huge-spacer-top"
          onCheck={() => props.onToggleSpecificSetting(true)}
          value="specific"
        >
          {translate('project_baseline.specific_setting')}
        </RadioButton>
      </div>

      <div className="big-spacer-left big-spacer-right project-baseline-setting">
        {newCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis && (
          <NewCodeDefinitionAnalysisWarning />
        )}
        <div className="display-flex-column big-spacer-bottom sw-gap-4" role="radiogroup">
          <NewCodeDefinitionPreviousVersionOption
            disabled={!overrideGlobalNewCodeDefinition}
            onSelect={props.onSelectSetting}
            selected={
              overrideGlobalNewCodeDefinition &&
              selectedNewCodeDefinitionType === NewCodeDefinitionType.PreviousVersion
            }
          />
          <NewCodeDefinitionDaysOption
            days={days}
            currentDaysValue={
              newCodeDefinitionType === NewCodeDefinitionType.NumberOfDays
                ? newCodeDefinitionValue
                : undefined
            }
            previousNonCompliantValue={previousNonCompliantValue}
            updatedAt={projectNcdUpdatedAt}
            disabled={!overrideGlobalNewCodeDefinition}
            isChanged={isChanged}
            isValid={isValid}
            onChangeDays={props.onSelectDays}
            onSelect={props.onSelectSetting}
            selected={
              overrideGlobalNewCodeDefinition &&
              selectedNewCodeDefinitionType === NewCodeDefinitionType.NumberOfDays
            }
            settingLevel={NewCodeDefinitionLevels.Project}
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
              onSelect={noop}
              selected={
                overrideGlobalNewCodeDefinition &&
                selectedNewCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis
              }
            />
          )}
        </div>
        {!branchesEnabled &&
          overrideGlobalNewCodeDefinition &&
          selectedNewCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis && (
            <BranchAnalysisList
              analysis={analysis ?? ''}
              branch={branch.name}
              component={component}
              onSelectAnalysis={noop}
            />
          )}
      </div>
      <div className="big-spacer-top">
        <Alert variant="info" className={classNames('spacer-bottom', { invisible: !isChanged })}>
          {translate('baseline.next_analysis_notice')}
        </Alert>
        <Spinner className="spacer-right" loading={saving} />
        {!saving && (
          <>
            <SubmitButton disabled={!isValid || !isChanged}>{translate('save')}</SubmitButton>
            <ResetButtonLink className="spacer-left" disabled={!isChanged} onClick={props.onCancel}>
              {translate('cancel')}
            </ResetButtonLink>
          </>
        )}
      </div>
    </form>
  );
}
