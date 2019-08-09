/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { validateDays } from '../utils';
import BaselineSettingDays from './BaselineSettingDays';
import BaselineSettingPreviousVersion from './BaselineSettingPreviousVersion';

export interface ProjectBaselineSelectorProps {
  currentSetting?: T.NewCodePeriodSettingType;
  currentSettingValue?: string | number;
  days: string;
  generalSetting?: { type: T.NewCodePeriodSettingType; value?: string };
  onSelectDays: (value: string) => void;
  onSelectSetting: (value: T.NewCodePeriodSettingType) => void;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  saving: boolean;
  selected?: T.NewCodePeriodSettingType;
}

export default function ProjectBaselineSelector(props: ProjectBaselineSelectorProps) {
  const { currentSetting, days, currentSettingValue, saving, selected } = props;

  const isChanged =
    selected !== currentSetting ||
    (selected === 'NUMBER_OF_DAYS' && String(days) !== currentSettingValue);

  const isValid = selected !== 'NUMBER_OF_DAYS' || validateDays(days);

  return (
    <form className="project-baseline-selector" onSubmit={props.onSubmit}>
      <div className="display-flex-row big-spacer-bottom" role="radiogroup">
        <BaselineSettingPreviousVersion
          onSelect={props.onSelectSetting}
          selected={selected === 'PREVIOUS_VERSION'}
        />
        <BaselineSettingDays
          days={days}
          isChanged={isChanged}
          isValid={isValid}
          onChangeDays={props.onSelectDays}
          onSelect={props.onSelectSetting}
          selected={selected === 'NUMBER_OF_DAYS'}
        />
      </div>
      {isChanged && (
        <div>
          <p className="spacer-bottom">{translate('baseline.next_analysis_notice')}</p>
          <DeferredSpinner className="spacer-right" loading={saving} />
          <SubmitButton disabled={saving || !isValid}>{translate('save')}</SubmitButton>
        </div>
      )}
    </form>
  );
}
