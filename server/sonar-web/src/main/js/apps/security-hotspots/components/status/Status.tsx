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
import withCurrentUserContext from '../../../../app/components/current-user/withCurrentUserContext';
import { Button } from '../../../../components/controls/buttons';
import { DropdownOverlay } from '../../../../components/controls/Dropdown';
import Toggler from '../../../../components/controls/Toggler';
import Tooltip from '../../../../components/controls/Tooltip';
import DropdownIcon from '../../../../components/icons/DropdownIcon';
import { PopupPlacement } from '../../../../components/ui/popups';
import { translate } from '../../../../helpers/l10n';
import { Hotspot, HotspotStatusOption } from '../../../../types/security-hotspots';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import { getStatusOptionFromStatusAndResolution } from '../../utils';
import StatusDescription from './StatusDescription';
import StatusSelection from './StatusSelection';

export interface StatusProps {
  currentUser: CurrentUser;
  hotspot: Hotspot;

  onStatusChange: (statusOption: HotspotStatusOption) => Promise<void>;
}

export function Status(props: StatusProps) {
  const { currentUser, hotspot } = props;

  const [isOpen, setIsOpen] = React.useState(false);
  const [comment, setComment] = React.useState('');

  React.useEffect(() => {
    setComment('');
  }, [hotspot.key]);

  const statusOption = getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution);
  const readonly = !hotspot.canChangeStatus || !isLoggedIn(currentUser);

  return (
    <div className="display-flex-row display-flex-end">
      <StatusDescription showTitle={true} statusOption={statusOption} />
      <div className="spacer-top">
        <Tooltip
          overlay={readonly ? translate('hotspots.status.cannot_change_status') : null}
          placement="bottom"
        >
          <div className="dropdown">
            <Toggler
              closeOnClickOutside={true}
              closeOnEscape={true}
              onRequestClose={() => setIsOpen(false)}
              open={isOpen}
              overlay={
                <DropdownOverlay noPadding={true} placement={PopupPlacement.Bottom}>
                  <StatusSelection
                    hotspot={hotspot}
                    onStatusOptionChange={async (status) => {
                      await props.onStatusChange(status);
                      setIsOpen(false);
                    }}
                    comment={comment}
                    setComment={setComment}
                  />
                </DropdownOverlay>
              }
            >
              <Button
                className="dropdown-toggle big-spacer-left"
                id="status-trigger"
                onClick={() => setIsOpen(true)}
                disabled={readonly}
              >
                <span>{translate('hotspots.status.select_status')}</span>
                <DropdownIcon className="little-spacer-left" />
              </Button>
            </Toggler>
          </div>
        </Tooltip>
      </div>
    </div>
  );
}

export default withCurrentUserContext(Status);
