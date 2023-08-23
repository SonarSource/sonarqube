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
import Tooltip from '../../../components/controls/Tooltip';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import GlobalNewCodeDefinitionDescription from '../../../components/new-code-definition/GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionDaysOption from '../../../components/new-code-definition/NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from '../../../components/new-code-definition/NewCodeDefinitionPreviousVersionOption';
import NewCodeDefinitionWarning from '../../../components/new-code-definition/NewCodeDefinitionWarning';
import { NewCodeDefinitionLevels } from '../../../components/new-code-definition/utils';
import { Alert } from '../../../components/ui/Alert';
import Spinner from '../../../components/ui/Spinner';
import { translate } from '../../../helpers/l10n';
import { isNewCodeDefinitionCompliant } from '../../../helpers/new-code-definition';
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
  canAdmin: boolean | undefined;
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
  onSelectSetting: (value?: NewCodeDefinitionType) => void;
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
    canAdmin,
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

  const isGlobalNcdCompliant = isNewCodeDefinitionCompliant(globalNewCodeDefinition);

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
          disabled={!isGlobalNcdCompliant}
          onCheck={() => props.onToggleSpecificSetting(false)}
          value="general"
        >
          <Tooltip
            overlay={
              isGlobalNcdCompliant
                ? null
                : translate('project_baseline.compliance.warning.title.global')
            }
          >
            <span>{translate('project_baseline.global_setting')}</span>
          </Tooltip>
        </RadioButton>

        <div className="sw-ml-4">
          <GlobalNewCodeDefinitionDescription
            globalNcd={globalNewCodeDefinition}
            isGlobalNcdCompliant={isGlobalNcdCompliant}
            canAdmin={canAdmin}
          />
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
        <NewCodeDefinitionWarning
          newCodeDefinitionType={newCodeDefinitionType}
          newCodeDefinitionValue={newCodeDefinitionValue}
          isBranchSupportEnabled={branchesEnabled}
          level={NewCodeDefinitionLevels.Project}
        />
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
              referenceBranch={referenceBranch || ''}
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
              analysis={analysis || ''}
              branch={branch.name}
              component={component}
              onSelectAnalysis={noop}
            />
          )}
      </div>
      <div className={classNames('big-spacer-top', { invisible: !isChanged })}>
        <Alert variant="info" className="spacer-bottom">
          {translate('baseline.next_analysis_notice')}
        </Alert>
        <Spinner className="spacer-right" loading={saving} />
        <SubmitButton disabled={saving || !isValid || !isChanged}>{translate('save')}</SubmitButton>
        <ResetButtonLink className="spacer-left" onClick={props.onCancel}>
          {translate('cancel')}
        </ResetButtonLink>
      </div>
    </form>
  );
}
