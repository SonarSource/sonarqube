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
import * as classnames from 'classnames';
import * as React from 'react';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Radio from 'sonar-ui-common/components/controls/Radio';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import MarkdownTips from '../../../components/common/MarkdownTips';
import {
  Hotspot,
  HotspotResolution,
  HotspotStatus,
  HotspotStatusOption
} from '../../../types/security-hotspots';

export interface HotspotActionsFormRendererProps {
  comment: string;
  hotspot: Hotspot;
  onAssign: (user: T.UserActive) => void;
  onChangeComment: (comment: string) => void;
  onSelectOption: (option: HotspotStatusOption) => void;
  onSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  selectedOption: HotspotStatusOption;
  selectedUser?: T.UserActive;
  submitting: boolean;
}

export default function HotspotActionsFormRenderer(props: HotspotActionsFormRendererProps) {
  const { comment, hotspot, selectedOption, submitting } = props;

  const disableStatusChange = !hotspot.canChangeStatus;

  return (
    <form className="abs-width-400 padded" onSubmit={props.onSubmit}>
      <h2>
        {disableStatusChange
          ? translate('hotspots.form.title.disabled')
          : translate('hotspots.form.title')}
      </h2>
      <div className="display-flex-column big-spacer-bottom">
        {renderOption({
          disabled: disableStatusChange,
          option: HotspotStatusOption.FIXED,
          selectedOption,
          onClick: props.onSelectOption
        })}
        {renderOption({
          disabled: disableStatusChange,
          option: HotspotStatusOption.SAFE,
          selectedOption,
          onClick: props.onSelectOption
        })}
        {renderOption({
          disabled: disableStatusChange,
          option: HotspotStatusOption.ADDITIONAL_REVIEW,
          selectedOption,
          onClick: props.onSelectOption
        })}
      </div>
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
        <SubmitButton disabled={submitting || !changes(props)}>
          {translate('hotspots.form.submit', hotspot.status)}
        </SubmitButton>
      </div>
    </form>
  );
}

const noop = () => {};

function changes(params: {
  comment: string;
  hotspot: Hotspot;
  selectedOption: HotspotStatusOption;
  selectedUser?: T.UserActive;
}) {
  const { comment, hotspot, selectedOption, selectedUser } = params;

  const status =
    selectedOption === HotspotStatusOption.ADDITIONAL_REVIEW
      ? HotspotStatus.TO_REVIEW
      : HotspotStatus.REVIEWED;

  const resolution =
    selectedOption !== HotspotStatusOption.ADDITIONAL_REVIEW
      ? HotspotResolution[selectedOption]
      : undefined;

  return (
    comment.length > 0 ||
    selectedUser ||
    status !== hotspot.status ||
    resolution !== hotspot.resolution
  );
}

function renderOption(params: {
  disabled: boolean;
  option: HotspotStatusOption;
  onClick: (option: HotspotStatusOption) => void;
  selectedOption: HotspotStatusOption;
}) {
  const { disabled, onClick, option, selectedOption } = params;

  const optionRender = (
    <div className="big-spacer-top">
      <Radio
        checked={selectedOption === option}
        className={classnames({ disabled })}
        onCheck={disabled ? noop : onClick}
        value={option}>
        <h3 className={classnames({ 'text-muted': disabled })}>
          {translate('hotspots.status_option', option)}
        </h3>
      </Radio>
      <div className={classnames('radio-button-description', { 'text-muted': disabled })}>
        {translate('hotspots.status_option', option, 'description')}
      </div>
    </div>
  );

  return disabled ? (
    <Tooltip overlay={translate('hotspots.form.cannot_change_status')} placement="left">
      {optionRender}
    </Tooltip>
  ) : (
    optionRender
  );
}
