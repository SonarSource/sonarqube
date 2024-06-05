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
  ActionsDropdown,
  ItemButton,
  ItemDangerButton,
  ItemDivider,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Provider } from '../../../types/types';
import { RestUserDetailed, isUserActive } from '../../../types/users';
import DeactivateForm from './DeactivateForm';
import PasswordForm from './PasswordForm';
import UserForm from './UserForm';

interface Props {
  manageProvider: Provider | undefined;
  user: RestUserDetailed;
}

export default function UserActions(props: Props) {
  const { user, manageProvider } = props;

  const [openForm, setOpenForm] = React.useState<string | undefined>(undefined);

  const isInstanceManaged = manageProvider !== undefined;

  const isUserLocal = isInstanceManaged && !user.managed;

  return (
    <>
      <ActionsDropdown
        id={`user-settings-action-dropdown-${user.login}`}
        toggleClassName="it__user-actions-toggle"
        allowResizing
        ariaLabel={translateWithParameters('users.manage_user', user.login)}
        zLevel={PopupZLevel.Global}
      >
        <ItemButton className="it__user-update" onClick={() => setOpenForm('update')}>
          {isInstanceManaged ? translate('update_scm') : translate('update_details')}
        </ItemButton>
        {!isInstanceManaged && user.local && (
          <ItemButton className="it__user-change-password" onClick={() => setOpenForm('password')}>
            {translate('my_profile.password.title')}
          </ItemButton>
        )}
        {isUserActive(user) && !isInstanceManaged && <ItemDivider />}
        {isUserActive(user) && (!isInstanceManaged || isUserLocal) && (
          <ItemDangerButton
            className="it__user-deactivate"
            onClick={() => setOpenForm('deactivate')}
          >
            {translate('users.deactivate')}
          </ItemDangerButton>
        )}
      </ActionsDropdown>
      {openForm === 'deactivate' && isUserActive(user) && (
        <DeactivateForm onClose={() => setOpenForm(undefined)} user={user} />
      )}
      {openForm === 'password' && (
        <PasswordForm onClose={() => setOpenForm(undefined)} user={user} />
      )}
      {openForm === 'update' && (
        <UserForm
          onClose={() => setOpenForm(undefined)}
          user={user}
          isInstanceManaged={isInstanceManaged}
        />
      )}
    </>
  );
}
