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
import * as React from 'react';
import FormattingTips from '../../../../components/common/FormattingTips';
import { SubmitButton } from '../../../../components/controls/buttons';
import Radio from '../../../../components/controls/Radio';
import { translate } from '../../../../helpers/l10n';
import { HotspotStatusOption } from '../../../../types/security-hotspots';
import StatusDescription from './StatusDescription';

export interface StatusSelectionRendererProps {
  selectedStatus: HotspotStatusOption;
  onStatusChange: (statusOption: HotspotStatusOption) => void;

  comment?: string;
  onCommentChange: (comment: string) => void;

  onSubmit: () => void;

  loading: boolean;
  submitDisabled: boolean;
}

export default function StatusSelectionRenderer(props: StatusSelectionRendererProps) {
  const { comment, loading, selectedStatus, submitDisabled } = props;

  const renderOption = (status: HotspotStatusOption) => {
    return (
      <Radio
        checked={selectedStatus === status}
        className="big-spacer-bottom status-radio"
        alignLabel={true}
        onCheck={props.onStatusChange}
        value={status}
      >
        <StatusDescription statusOption={status} statusInBadge={false} />
      </Radio>
    );
  };

  return (
    <div data-testid="security-hotspot-test" className="abs-width-400">
      <div className="big-padded">
        {renderOption(HotspotStatusOption.TO_REVIEW)}
        {renderOption(HotspotStatusOption.ACKNOWLEDGED)}
        {renderOption(HotspotStatusOption.FIXED)}
        {renderOption(HotspotStatusOption.SAFE)}
      </div>

      <hr />
      <div className="big-padded display-flex-column">
        <label className="text-bold" htmlFor="comment-textarea">
          {translate('hotspots.status.add_comment')}
        </label>
        <textarea
          className="spacer-top form-field fixed-width spacer-bottom"
          id="comment-textarea"
          onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
            props.onCommentChange(event.currentTarget.value)
          }
          rows={4}
          value={comment}
        />
        <FormattingTips />

        <div className="big-spacer-top display-flex-justify-end display-flex-center">
          <SubmitButton disabled={submitDisabled || loading} onClick={props.onSubmit}>
            {translate('hotspots.status.change_status')}
          </SubmitButton>

          {loading && <i className="spacer-left spinner" />}
        </div>
      </div>
    </div>
  );
}
