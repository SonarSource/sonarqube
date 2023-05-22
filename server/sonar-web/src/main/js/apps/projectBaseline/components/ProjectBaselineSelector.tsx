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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Link from '../../../components/common/Link';
import Radio from '../../../components/controls/Radio';
import Tooltip from '../../../components/controls/Tooltip';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import NewCodeDefinitionWarning from '../../../components/new-code-definition/NewCodeDefinitionWarning';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isNewCodeDefinitionCompliant } from '../../../helpers/periods';
import { Branch } from '../../../types/branch-like';
import { ParsedAnalysis } from '../../../types/project-activity';
import { NewCodePeriod, NewCodePeriodSettingType } from '../../../types/types';
import { validateSetting } from '../utils';
import BaselineSettingAnalysis from './BaselineSettingAnalysis';
import BaselineSettingDays from './BaselineSettingDays';
import BaselineSettingPreviousVersion from './BaselineSettingPreviousVersion';
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

function renderGeneralSetting(generalSetting: NewCodePeriod) {
  let setting: string;
  let description: string;
  let useCase: string;
  if (generalSetting.type === NewCodePeriodSettingType.NUMBER_OF_DAYS) {
    setting = `${translate('baseline.number_days')} (${translateWithParameters(
      'duration.days',
      generalSetting.value || '?'
    )})`;
    description = translate('baseline.number_days.description');
    useCase = translate('baseline.number_days.usecase');
  } else {
    setting = translate('baseline.previous_version');
    description = translate('baseline.previous_version.description');
    useCase = translate('baseline.previous_version.usecase');
  }

  return (
    <div className="general-setting display-flex-start">
      <span className="sw-font-bold flex-0">{setting}:&nbsp;</span>
      <span>
        {description} {useCase}
      </span>
    </div>
  );
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

  const isGeneralSettingCompliant = isNewCodeDefinitionCompliant(generalSetting);

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
        <Radio
          checked={!overrideGeneralSetting}
          className="big-spacer-bottom"
          disabled={!isGeneralSettingCompliant}
          onCheck={() => props.onToggleSpecificSetting(false)}
          value="general"
        >
          <Tooltip
            overlay={
              isGeneralSettingCompliant
                ? null
                : translate('project_baseline.compliance.warning.title.global')
            }
          >
            <span>{translate('project_baseline.global_setting')}</span>
          </Tooltip>
        </Radio>

        <div className="big-spacer-left">
          {!isGeneralSettingCompliant && (
            <Alert variant="warning" className="sw-mb-4 sw-max-w-[800px]">
              <p className="sw-mb-2 sw-font-bold">
                {translate('project_baseline.compliance.warning.title.global')}
              </p>
              <p className="sw-mb-2">
                {canAdmin ? (
                  <FormattedMessage
                    id="project_baseline.compliance.warning.explanation.admin"
                    defaultMessage={translate(
                      'project_baseline.compliance.warning.explanation.admin'
                    )}
                    values={{
                      link: (
                        <Link to="/admin/settings?category=new_code_period">
                          {translate('project_baseline.warning.explanation.action.admin.link')}
                        </Link>
                      ),
                    }}
                  />
                ) : (
                  translate('project_baseline.compliance.warning.explanation')
                )}
              </p>
            </Alert>
          )}
          {renderGeneralSetting(generalSetting)}
        </div>

        <Radio
          checked={overrideGeneralSetting}
          className="huge-spacer-top"
          onCheck={() => props.onToggleSpecificSetting(true)}
          value="specific"
        >
          {translate('project_baseline.specific_setting')}
        </Radio>
      </div>

      <div className="big-spacer-left big-spacer-right project-baseline-setting">
        <NewCodeDefinitionWarning
          newCodeDefinitionType={currentSetting}
          newCodeDefinitionValue={currentSettingValue}
          isBranchSupportEnabled={branchesEnabled}
          level="project"
        />
        <div className="display-flex-row big-spacer-bottom" role="radiogroup">
          <BaselineSettingPreviousVersion
            disabled={!overrideGeneralSetting}
            onSelect={props.onSelectSetting}
            selected={
              overrideGeneralSetting && selected === NewCodePeriodSettingType.PREVIOUS_VERSION
            }
          />
          <BaselineSettingDays
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
