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
import MarkdownTips from '../../../components/common/MarkdownTips';
import { HotspotStatus, HotspotStatusOption } from '../../../types/security-hotspots';
import HotspotAssigneeSelect from './HotspotAssigneeSelect';

export interface HotspotActionsFormRendererProps {
  comment: string;
  hotspotStatus: HotspotStatus;
  onAssign: (user: T.UserActive) => void;
  onChangeComment: (comment: string) => void;
  onSelectOption: (option: HotspotStatusOption) => void;
  onSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  selectedOption: HotspotStatusOption;
  selectedUser?: T.UserActive;
  submitting: boolean;
}

export default function HotspotActionsFormRenderer(props: HotspotActionsFormRendererProps) {
  const { comment, hotspotStatus, selectedOption, submitting } = props;

  return (
    <form className="abs-width-400 padded" onSubmit={props.onSubmit}>
      <h2>{translate('hotspots.form.title')}</h2>
      <div className="display-flex-column big-spacer-bottom">
        {renderOption({
          option: HotspotStatusOption.FIXED,
          selectedOption,
          onClick: props.onSelectOption
        })}
        {renderOption({
          option: HotspotStatusOption.SAFE,
          selectedOption,
          onClick: props.onSelectOption
        })}
        {renderOption({
          option: HotspotStatusOption.ADDITIONAL_REVIEW,
          selectedOption,
          onClick: props.onSelectOption
        })}
      </div>
      {selectedOption === HotspotStatusOption.ADDITIONAL_REVIEW && (
        <div className="form-field huge-spacer-left">
          <label>{translate('hotspots.form.assign_to')}</label>
          <HotspotAssigneeSelect onSelect={props.onAssign} />
        </div>
      )}
      <div className="display-flex-column big-spacer-bottom">
        <label className="little-spacer-bottom">{translate('hotspots.form.comment')}</label>
        <textarea
          autoFocus={true}
          className="form-field fixed-width spacer-bottom"
          onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
            props.onChangeComment(event.currentTarget.value)
          }
          placeholder={
            selectedOption === HotspotStatusOption.SAFE
              ? translate('hotspots.form.comment.placeholder')
              : ''
          }
          rows={6}
          value={comment}
        />
        <MarkdownTips />
      </div>
      <div className="text-right">
        {submitting && <i className="spinner spacer-right" />}
        <SubmitButton disabled={submitting}>
          {translate('hotspots.form.submit', hotspotStatus)}
        </SubmitButton>
      </div>
    </form>
  );
}

function renderOption(params: {
  option: HotspotStatusOption;
  onClick: (option: HotspotStatusOption) => void;
  selectedOption: HotspotStatusOption;
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
