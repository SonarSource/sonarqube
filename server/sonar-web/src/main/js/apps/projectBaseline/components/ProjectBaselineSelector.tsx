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
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import GlobalNewCodeDefinitionDescription from '../../../components/new-code-definition/GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionDaysOption from '../../../components/new-code-definition/NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from '../../../components/new-code-definition/NewCodeDefinitionPreviousVersionOption';
import NewCodeDefinitionWarning from '../../../components/new-code-definition/NewCodeDefinitionWarning';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { isNewCodeDefinitionCompliant } from '../../../helpers/periods';
import { Branch } from '../../../types/branch-like';
import { ParsedAnalysis } from '../../../types/project-activity';
import { NewCodePeriod, NewCodePeriodSettingType } from '../../../types/types';
import { validateSetting } from '../utils';
import BaselineSettingAnalysis from './BaselineSettingAnalysis';
import BaselineSettingReferenceBranch from './BaselineSettingReferenceBranch';
import BranchAnalysisList from './BranchAnalysisList';

export interface ProjectBaselineSelectorProps {
  analysis?: string;
  branch: Branch;
  branchList: Branch[];
  branchesEnabled?: boolean;
  canAdmin: boolean | undefined;
  component: string;
  currentSetting?: NewCodePeriodSettingType;
  currentSettingValue?: string;
  days: string;
  generalSetting: NewCodePeriod;
  onCancel: () => void;
  onSelectAnalysis: (analysis: ParsedAnalysis) => void;
  onSelectDays: (value: string) => void;
  onSelectReferenceBranch: (value: string) => void;
  onSelectSetting: (value?: NewCodePeriodSettingType) => void;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  onToggleSpecificSetting: (selection: boolean) => void;
  referenceBranch?: string;
  saving: boolean;
  selected?: NewCodePeriodSettingType;
  overrideGeneralSetting: boolean;
}

function branchToOption(b: Branch) {
  return { label: b.name, value: b.name, isMain: b.isMain };
}

export default function ProjectBaselineSelector(props: ProjectBaselineSelectorProps) {
  const {
    analysis,
    branch,
    branchList,
    branchesEnabled,
    canAdmin,
    component,
    currentSetting,
    currentSettingValue,
    days,
    generalSetting,
    overrideGeneralSetting,
    referenceBranch,
    saving,
    selected,
  } = props;

  const isGlobalNcdCompliant = isNewCodeDefinitionCompliant(generalSetting);

  const { isChanged, isValid } = validateSetting({
    analysis,
    currentSetting,
    currentSettingValue,
    days,
    overrideGeneralSetting,
    referenceBranch,
    selected,
  });

  return (
    <form className="project-baseline-selector" onSubmit={props.onSubmit}>
      <div className="big-spacer-top spacer-bottom" role="radiogroup">
        <RadioButton
          checked={!overrideGeneralSetting}
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
            globalNcd={generalSetting}
            isGlobalNcdCompliant={isGlobalNcdCompliant}
            canAdmin={canAdmin}
          />
        </div>

        <RadioButton
          checked={overrideGeneralSetting}
          className="huge-spacer-top"
          onCheck={() => props.onToggleSpecificSetting(true)}
          value="specific"
        >
          {translate('project_baseline.specific_setting')}
        </RadioButton>
      </div>

      <div className="big-spacer-left big-spacer-right project-baseline-setting">
        <NewCodeDefinitionWarning
          newCodeDefinitionType={currentSetting}
          newCodeDefinitionValue={currentSettingValue}
          isBranchSupportEnabled={branchesEnabled}
          level="project"
        />
        <div className="display-flex-row big-spacer-bottom" role="radiogroup">
          <NewCodeDefinitionPreviousVersionOption
            disabled={!overrideGeneralSetting}
            onSelect={props.onSelectSetting}
            selected={
              overrideGeneralSetting && selected === NewCodePeriodSettingType.PREVIOUS_VERSION
            }
          />
          <NewCodeDefinitionDaysOption
            days={days}
            disabled={!overrideGeneralSetting}
            isChanged={isChanged}
            isValid={isValid}
            onChangeDays={props.onSelectDays}
            onSelect={props.onSelectSetting}
            selected={
              overrideGeneralSetting && selected === NewCodePeriodSettingType.NUMBER_OF_DAYS
            }
          />
          {branchesEnabled && (
            <BaselineSettingReferenceBranch
              branchList={branchList.map(branchToOption)}
              disabled={!overrideGeneralSetting}
              onChangeReferenceBranch={props.onSelectReferenceBranch}
              onSelect={props.onSelectSetting}
              referenceBranch={referenceBranch || ''}
              selected={
                overrideGeneralSetting && selected === NewCodePeriodSettingType.REFERENCE_BRANCH
              }
              settingLevel="project"
            />
          )}
          {!branchesEnabled && currentSetting === NewCodePeriodSettingType.SPECIFIC_ANALYSIS && (
            <BaselineSettingAnalysis
              onSelect={() => {}}
              selected={
                overrideGeneralSetting && selected === NewCodePeriodSettingType.SPECIFIC_ANALYSIS
              }
            />
          )}
        </div>
        {!branchesEnabled &&
          overrideGeneralSetting &&
          selected === NewCodePeriodSettingType.SPECIFIC_ANALYSIS && (
            <BranchAnalysisList
              analysis={analysis || ''}
              branch={branch.name}
              component={component}
              onSelectAnalysis={props.onSelectAnalysis}
            />
          )}
      </div>
      <div className={classNames('big-spacer-top', { invisible: !isChanged })}>
        <Alert variant="info" className="spacer-bottom">
          {translate('baseline.next_analysis_notice')}
        </Alert>
        <DeferredSpinner className="spacer-right" loading={saving} />
        <SubmitButton disabled={saving || !isValid || !isChanged}>{translate('save')}</SubmitButton>
        <ResetButtonLink className="spacer-left" onClick={props.onCancel}>
          {translate('cancel')}
        </ResetButtonLink>
      </div>
    </form>
  );
}
