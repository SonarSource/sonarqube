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
import * as React from 'react';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Radio from 'sonar-ui-common/components/controls/Radio';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { HotspotStatusOptions } from '../../../types/security-hotspots';

export interface HotspotActionsFormRendererProps {
  hotspotKey: string;
  onSelectOption: (option: HotspotStatusOptions) => void;
  onSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  selectedOption: HotspotStatusOptions;
  submitting: boolean;
}

export default function HotspotActionsFormRenderer(props: HotspotActionsFormRendererProps) {
  const { selectedOption, submitting } = props;

  return (
    <form className="abs-width-400" onSubmit={props.onSubmit}>
      <h2>{translate('hotspots.form.title')}</h2>
      <div className="display-flex-column big-spacer-bottom">
        {renderOption({
          option: HotspotStatusOptions.FIXED,
          selectedOption,
          onClick: props.onSelectOption
        })}
        {renderOption({
          option: HotspotStatusOptions.SAFE,
          selectedOption,
          onClick: props.onSelectOption
        })}
        {renderOption({
          option: HotspotStatusOptions.ADDITIONAL_REVIEW,
          selectedOption,
          onClick: props.onSelectOption
        })}
      </div>
      <div className="text-right">
        {submitting && <i className="spinner spacer-right" />}
        <SubmitButton disabled={submitting}>{translate('hotspots.form.submit')}</SubmitButton>
      </div>
    </form>
  );
}

function renderOption(params: {
  option: HotspotStatusOptions;
  onClick: (option: HotspotStatusOptions) => void;
  selectedOption: HotspotStatusOptions;
}) {
  const { onClick, option, selectedOption } = params;
  return (
    <div className="big-spacer-top">
      <Radio checked={selectedOption === option} onCheck={onClick} value={option}>
        <h3>{translate('hotspots.status_option', option)}</h3>
      </Radio>
      <div className="radio-button-description">
        {translate('hotspots.status_option', option, 'description')}
      </div>
    </div>
  );
}
