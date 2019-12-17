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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import OutsideClickHandler from 'sonar-ui-common/components/controls/OutsideClickHandler';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { HotspotUpdateFields } from '../../../types/security-hotspots';
import HotspotActionsForm from './HotspotActionsForm';

export interface HotspotActionsProps {
  hotspotKey: string;
  onSubmit: (hotspot: HotspotUpdateFields) => void;
}

const ESCAPE_KEY = 'Escape';

export default function HotspotActions(props: HotspotActionsProps) {
  const [open, setOpen] = React.useState(false);

  React.useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === ESCAPE_KEY) {
        setOpen(false);
      }
    };

    document.addEventListener('keydown', handleKeyDown, false);

    return () => {
      document.removeEventListener('keydown', handleKeyDown, false);
    };
  });

  return (
    <div className="dropdown">
      <Button onClick={() => setOpen(!open)}>
        {translate('hotspots.review_hotspot')}
        <DropdownIcon className="little-spacer-left" />
      </Button>

      {open && (
        <OutsideClickHandler onClickOutside={() => setOpen(false)}>
          <DropdownOverlay placement={PopupPlacement.BottomRight}>
            <HotspotActionsForm
              hotspotKey={props.hotspotKey}
              onSubmit={data => {
                setOpen(false);
                props.onSubmit(data);
              }}
            />
          </DropdownOverlay>
        </OutsideClickHandler>
      )}
    </div>
  );
}
