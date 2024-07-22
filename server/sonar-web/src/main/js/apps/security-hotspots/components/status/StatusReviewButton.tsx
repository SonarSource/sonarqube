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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import withCurrentUserContext from '../../../../app/components/current-user/withCurrentUserContext';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate } from '../../../../helpers/l10n';
import { Hotspot, HotspotStatusOption } from '../../../../types/security-hotspots';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import StatusSelection from './StatusSelection';

export interface StatusProps {
  currentUser: CurrentUser;
  hotspot: Hotspot;
  onStatusChange: (statusOption: HotspotStatusOption) => Promise<void>;
}

export function StatusReviewButton(props: StatusProps) {
  const { currentUser, hotspot } = props;

  const [isOpen, setIsOpen] = React.useState(false);
  const readonly = !hotspot.canChangeStatus || !isLoggedIn(currentUser);

  return (
    <>
      <Tooltip
        content={readonly ? translate('hotspots.status.cannot_change_status') : null}
        side="bottom"
      >
        <Button
          id="status-trigger"
          onClick={() => setIsOpen(true)}
          isDisabled={readonly}
          variety={ButtonVariety.Primary}
        >
          {translate('hotspots.status.review')}
        </Button>
      </Tooltip>

      {isOpen && (
        <StatusSelection
          hotspot={hotspot}
          onClose={() => setIsOpen(false)}
          onStatusOptionChange={props.onStatusChange}
        />
      )}
    </>
  );
}

export default withCurrentUserContext(StatusReviewButton);
