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
import classNames from 'classnames';
import * as React from 'react';
import { ClearButton } from '../../../../../components/controls/buttons';
import EscKeydownHandler from '../../../../../components/controls/EscKeydownHandler';
import OutsideClickHandler from '../../../../../components/controls/OutsideClickHandler';
import { translate } from '../../../../../helpers/l10n';
import './InfoDrawer.css';

export interface InfoDrawerProps {
  children: React.ReactNode;
  displayed: boolean;
  onClose: () => void;
  top: number;
}

export default function InfoDrawer(props: InfoDrawerProps) {
  const { children, displayed, onClose, top } = props;

  return (
    <div
      className={classNames('info-drawer info-drawer-pane', { open: displayed })}
      style={{ top }}
    >
      {displayed && (
        <>
          <div className="close-button">
            <ClearButton aria-label={translate('close')} onClick={onClose} />
          </div>
          <EscKeydownHandler onKeydown={onClose}>
            <OutsideClickHandler onClickOutside={onClose}>
              <div className="display-flex-column max-height-100">{children}</div>
            </OutsideClickHandler>
          </EscKeydownHandler>
        </>
      )}
    </div>
  );
}
