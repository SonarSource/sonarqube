/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Radio from 'sonar-ui-common/components/controls/Radio';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { validateSetting } from '../utils';
import BaselineSettingAnalysis from './BaselineSettingAnalysis';
import BaselineSettingDays from './BaselineSettingDays';
import BaselineSettingPreviousVersion from './BaselineSettingPreviousVersion';
import BranchAnalysisList from './BranchAnalysisList';

export interface ProjectBaselineSelectorProps {
  analysis?: string;
  branchesEnabled?: boolean;
  component: string;
  currentSetting?: T.NewCodePeriodSettingType;
  currentSettingValue?: string;
  days: string;
  generalSetting: T.NewCodePeriod;
  onCancel: () => void;
  onSelectAnalysis: (analysis: T.ParsedAnalysis) => void;
  onSelectDays: (value: string) => void;
  onSelectSetting: (value?: T.NewCodePeriodSettingType) => void;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  onToggleSpecificSetting: (selection: boolean) => void;
  saving: boolean;
  selected?: T.NewCodePeriodSettingType;
  overrideGeneralSetting: boolean;
}

function renderGeneralSetting(generalSetting: T.NewCodePeriod) {
  let setting: string;
  let description: string;
  if (generalSetting.type === 'NUMBER_OF_DAYS') {
    setting = `${translate('baseline.number_days')} (${translateWithParameters(
      'duration.days',
      generalSetting.value || '?'
    )})`;
    description = translate('baseline.number_days.description');
  } else {
    setting = translate('baseline.previous_version');
    description = translate('baseline.previous_version.description');
  }

  return (
    <div className="general-setting">
      <strong>{setting}</strong>: {description}
    </div>
  );
}

export default function ProjectBaselineSelector(props: ProjectBaselineSelectorProps) {
  const {
    analysis,
    branchesEnabled,
    component,
    currentSetting,
    currentSettingValue,
    days,
    generalSetting,
    saving,
    selected,
    overrideGeneralSetting
  } = props;

  const { isChanged, isValid } = validateSetting({
    analysis,
    currentSetting,
    currentSettingValue,
    days,
    selected,
    overrideGeneralSetting
  });

  return (
    <form className="project-baseline-selector" onSubmit={props.onSubmit}>
      <div className="big-spacer-top spacer-bottom" role="radiogroup">
        <Radio
          checked={!overrideGeneralSetting}
          className="big-spacer-bottom"
          onCheck={() => props.onToggleSpecificSetting(false)}
          value="general">
          {translate('project_baseline.general_setting')}
        </Radio>
        <div className="big-spacer-left">{renderGeneralSetting(generalSetting)}</div>

        <Radio
          checked={overrideGeneralSetting}
          className="huge-spacer-top"
          onCheck={() => props.onToggleSpecificSetting(true)}
          value="specific">
          {translate('project_baseline.specific_setting')}
        </Radio>
      </div>

      <div className="big-spacer-left big-spacer-right branch-baseline-setting-modal">
        <div className="display-flex-row big-spacer-bottom" role="radiogroup">
          <BaselineSettingPreviousVersion
            disabled={!overrideGeneralSetting}
            onSelect={props.onSelectSetting}
            selected={overrideGeneralSetting && selected === 'PREVIOUS_VERSION'}
          />
          <BaselineSettingDays
            days={days}
            disabled={!overrideGeneralSetting}
            isChanged={isChanged}
            isValid={isValid}
            onChangeDays={props.onSelectDays}
            onSelect={props.onSelectSetting}
            selected={overrideGeneralSetting && selected === 'NUMBER_OF_DAYS'}
          />
          {!branchesEnabled && (
            <BaselineSettingAnalysis
              disabled={!overrideGeneralSetting}
              onSelect={props.onSelectSetting}
              selected={overrideGeneralSetting && selected === 'SPECIFIC_ANALYSIS'}
            />
          )}
        </div>
        {selected === 'SPECIFIC_ANALYSIS' && (
          <BranchAnalysisList
            analysis={analysis || ''}
            branch="master"
            component={component}
            onSelectAnalysis={props.onSelectAnalysis}
          />
        )}
      </div>
      <div className={classNames('big-spacer-top', { invisible: !isChanged })}>
        <p className="spacer-bottom">{translate('baseline.next_analysis_notice')}</p>
        <DeferredSpinner className="spacer-right" loading={saving} />
        <SubmitButton disabled={saving || !isValid || !isChanged}>{translate('save')}</SubmitButton>
        <ResetButtonLink className="spacer-left" onClick={props.onCancel}>
          {translate('cancel')}
        </ResetButtonLink>
      </div>
    </form>
  );
}
