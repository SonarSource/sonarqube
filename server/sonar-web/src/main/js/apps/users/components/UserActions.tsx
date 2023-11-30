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
import * as React from 'react';
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem,
} from '../../../components/controls/ActionsDropdown';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Provider } from '../../../types/types';
import { RestUserDetailed, isUserActive } from '../../../types/users';
import DeactivateForm from './DeactivateForm';
import PasswordForm from './PasswordForm';
import UserForm from './UserForm';

interface Props {
  user: RestUserDetailed;
  manageProvider: Provider | undefined;
}

export default function UserActions(props: Props) {
  const { user, manageProvider } = props;

  const [openForm, setOpenForm] = React.useState<string | undefined>(undefined);

  const isInstanceManaged = manageProvider !== undefined;

  const isUserLocal = isInstanceManaged && !user.managed;

  return (
    <>
      <ActionsDropdown label={translateWithParameters('users.manage_user', user.login)}>
        <ActionsDropdownItem className="js-user-update" onClick={() => setOpenForm('update')}>
          {isInstanceManaged ? translate('update_scm') : translate('update_details')}
        </ActionsDropdownItem>
        {!isInstanceManaged && user.local && (
          <ActionsDropdownItem
            className="js-user-change-password"
            onClick={() => setOpenForm('password')}
          >
            {translate('my_profile.password.title')}
          </ActionsDropdownItem>
        )}

        {isUserActive(user) && !isInstanceManaged && <ActionsDropdownDivider />}
        {isUserActive(user) && (!isInstanceManaged || isUserLocal) && (
          <ActionsDropdownItem
            className="js-user-deactivate"
            destructive
            onClick={() => setOpenForm('deactivate')}
          >
            {translate('users.deactivate')}
          </ActionsDropdownItem>
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
