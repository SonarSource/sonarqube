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
import {
  Dropdown,
  InteractiveIcon,
  MenuHelpIcon,
  PopupPlacement,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import Tooltip from '../controls/Tooltip';
import { EmbedDocsPopup } from './EmbedDocsPopup';

export default function EmbedDocsPopupHelper() {
  return (
    <div className="dropdown">
      <Dropdown
        id="help-menu-dropdown"
        placement={PopupPlacement.BottomRight}
        overlay={<EmbedDocsPopup />}
        allowResizing
        zLevel={PopupZLevel.Global}
      >
        {({ onToggleClick, open }) => (
          <Tooltip mouseLeaveDelay={0.2} overlay={!open ? translate('help') : undefined}>
            <InteractiveIcon
              Icon={MenuHelpIcon}
              aria-expanded={open}
              iconProps={{
                'data-guiding-id': 'issue-5',
              }}
              aria-controls="help-menu-dropdown"
              aria-haspopup
              aria-label={translate('help')}
              currentColor
              onClick={onToggleClick}
              size="medium"
              stopPropagation={false}
            />
          </Tooltip>
        )}
      </Dropdown>
    </div>
  );
}
