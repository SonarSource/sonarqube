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
  Avatar,
  BareButton,
  ButtonSecondary,
  Dropdown,
  PopupPlacement,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { GlobalSettingKeys } from '../../../../types/settings';
import { isLoggedIn } from '../../../../types/users';
import { AppStateContext } from '../../app-state/AppStateContext';
import { CurrentUserContext } from '../../current-user/CurrentUserContext';
import { GlobalNavUserMenu } from './GlobalNavUserMenu';

export function GlobalNavUser() {
  const userContext = React.useContext(CurrentUserContext);
  const currentUser = userContext?.currentUser;

  const { settings } = React.useContext(AppStateContext);

  const handleLogin = React.useCallback(() => {
    const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.href = `${getBaseUrl()}/sessions/new?return_to=${returnTo}${
      window.location.hash
    }`;
  }, []);

  if (!currentUser || !isLoggedIn(currentUser)) {
    return (
      <div>
        <ButtonSecondary onClick={handleLogin}>{translate('layout.login')}</ButtonSecondary>
      </div>
    );
  }

  const enableGravatar = settings[GlobalSettingKeys.EnableGravatar] === 'true';
  const gravatarServerUrl = settings[GlobalSettingKeys.GravatarServerUrl] ?? '';

  return (
    <Dropdown
      id="userAccountMenuDropdown"
      placement={PopupPlacement.BottomRight}
      zLevel={PopupZLevel.Global}
      overlay={<GlobalNavUserMenu currentUser={currentUser} />}
    >
      {({ a11yAttrs: { role, ...a11yAttrs }, onToggleClick, open }) => (
        <Tooltip
          mouseEnterDelay={0.2}
          overlay={!open ? translate('global_nav.account.tooltip') : undefined}
        >
          <BareButton
            aria-label={translate('global_nav.account.tooltip')}
            onClick={onToggleClick}
            {...a11yAttrs}
          >
            <Avatar
              enableGravatar={enableGravatar}
              gravatarServerUrl={gravatarServerUrl}
              hash={currentUser.avatar}
              name={currentUser.name}
            />
          </BareButton>
        </Tooltip>
      )}
    </Dropdown>
  );
}
