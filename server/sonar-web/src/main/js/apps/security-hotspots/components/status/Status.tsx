/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ButtonPrimary, HighlightedSection } from 'design-system';
import * as React from 'react';
import withCurrentUserContext from '../../../../app/components/current-user/withCurrentUserContext';
import Tooltip from '../../../../components/controls/Tooltip';
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
  const statusOption = getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution);
  const readonly = !hotspot.canChangeStatus || !isLoggedIn(currentUser);

  return (
    <>
      <HighlightedSection className="sw-flex sw-rounded-1 sw-p-4 sw-items-center sw-justify-between sw-gap-2 sw-flex-row">
        <StatusDescription statusOption={statusOption} />
        <Tooltip
          overlay={readonly ? translate('hotspots.status.cannot_change_status') : null}
          placement="bottom"
        >
          <ButtonPrimary id="status-trigger" onClick={() => setIsOpen(true)} disabled={readonly}>
            {translate('hotspots.status.review')}
          </ButtonPrimary>
        </Tooltip>
      </HighlightedSection>
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

export default withCurrentUserContext(Status);
