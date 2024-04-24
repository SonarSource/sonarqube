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
import { ButtonPrimary, ButtonSecondary, Checkbox, Modal, Note } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { save } from '../../../helpers/storage';
import { HotspotStatusOption } from '../../../types/security-hotspots';
import { SHOW_STATUS_DIALOG_STORAGE_KEY } from '../constants';

export interface StatusUpdateSuccessModalProps {
  hotspotsReviewedMeasure?: string;
  lastStatusChangedTo?: HotspotStatusOption;
  onClose: () => void;
  onSwitchFilterToStatusOfUpdatedHotspot: () => void;
}

export default function StatusUpdateSuccessModal(props: StatusUpdateSuccessModalProps) {
  const { hotspotsReviewedMeasure, lastStatusChangedTo } = props;

  const [isChecked, setIsChecked] = React.useState(false);

  if (!lastStatusChangedTo) {
    return null;
  }

  const closingHotspots = lastStatusChangedTo !== HotspotStatusOption.TO_REVIEW;
  const statusLabel = translate('hotspots.status_option', lastStatusChangedTo);
  const modalTitle = closingHotspots
    ? translate('hotspots.congratulations')
    : translate('hotspots.update.success');

  const handleCheckboxChange = (value: boolean) => {
    setIsChecked(value);
    save(SHOW_STATUS_DIALOG_STORAGE_KEY, (!value).toString());
  };

  return (
    <Modal onClose={props.onClose}>
      <p className="sw-hidden" id="modal_header_title">
        {modalTitle}
      </p>
      <h2 className="sw-heading-md sw-text-center">
        {translateWithParameters('hotspots.successful_status_change_to_x', statusLabel)}
      </h2>

      <div className="sw-text-center sw-mt-8 sw-body-sm">
        <FormattedMessage
          id="hotspots.successfully_changed_to_x"
          defaultMessage={translate('hotspots.find_in_status_filter_x')}
          values={{
            status_label: <strong>{statusLabel}</strong>,
          }}
        />
        {closingHotspots && (
          <p className="sw-mt-2">
            <FormattedMessage
              id="hotspots.x_done_keep_going"
              defaultMessage={translate('hotspots.x_done_keep_going')}
              values={{
                percentage: (
                  <strong>
                    {formatMeasure(hotspotsReviewedMeasure, 'PERCENT', {
                      omitExtraDecimalZeros: true,
                    })}
                  </strong>
                ),
              }}
            />
          </p>
        )}
      </div>
      <Checkbox checked={isChecked} onCheck={handleCheckboxChange} className="sw-mt-8">
        <Note className="sw-ml-2">{translate('hotspots.success_dialog.do_not_show')}</Note>
      </Checkbox>

      <div className="sw-flex sw-justify-between sw-mt-4">
        <ButtonSecondary
          onClick={() => {
            props.onSwitchFilterToStatusOfUpdatedHotspot();
            props.onClose();
          }}
        >
          {translateWithParameters('hotspots.see_x_hotspots', statusLabel)}
        </ButtonSecondary>
        <ButtonPrimary onClick={props.onClose}>
          {translate('hotspots.continue_to_next_hotspot')}
        </ButtonPrimary>
      </div>
    </Modal>
  );
}
