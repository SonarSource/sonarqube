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
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import ChevronDownIcon from 'sonar-ui-common/components/icons/ChevronDownIcon';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { withCurrentUser } from '../../../../components/hoc/withCurrentUser';
import { isLoggedIn } from '../../../../helpers/users';
import { Hotspot } from '../../../../types/security-hotspots';
import { getStatusOptionFromStatusAndResolution } from '../../utils';
import './Status.css';
import StatusDescription from './StatusDescription';
import StatusSelection from './StatusSelection';

export interface StatusProps {
  currentUser: T.CurrentUser;
  hotspot: Hotspot;

  onStatusChange: () => void;
}

export function Status(props: StatusProps) {
  const { currentUser, hotspot } = props;
  const [isOpen, setIsOpen] = React.useState(false);

  const statusOption = getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution);
  const readonly = !hotspot.canChangeStatus || !isLoggedIn(currentUser);

  const trigger = (
    <div
      aria-expanded={isOpen}
      aria-haspopup={true}
      className={classNames('padded bordered display-flex-column display-flex-justify-center', {
        readonly
      })}
      id="status-trigger"
      onClick={() => !readonly && setIsOpen(true)}
      role="button"
      tabIndex={0}>
      <div className="display-flex-center display-flex-space-between">
        {isOpen ? (
          <span className="h3">{translate('hotspots.status.select_status')}</span>
        ) : (
          <StatusDescription showTitle={true} statusOption={statusOption} />
        )}
        {!readonly && <ChevronDownIcon className="big-spacer-left" />}
      </div>
    </div>
  );

  const actionableTrigger = (
    <Toggler
      closeOnClickOutside={true}
      closeOnEscape={true}
      onRequestClose={() => setIsOpen(false)}
      open={isOpen}
      overlay={
        <DropdownOverlay noPadding={true} placement={PopupPlacement.Bottom}>
          <StatusSelection
            hotspot={hotspot}
            onStatusOptionChange={() => {
              setIsOpen(false);
              props.onStatusChange();
            }}
          />
        </DropdownOverlay>
      }>
      {trigger}
    </Toggler>
  );

  return (
    <div className="dropdown">
      {readonly ? (
        <Tooltip overlay={translate('hotspots.status.cannot_change_status')} placement="bottom">
          {actionableTrigger}
        </Tooltip>
      ) : (
        actionableTrigger
      )}
    </div>
  );
}

export default withCurrentUser(Status);
