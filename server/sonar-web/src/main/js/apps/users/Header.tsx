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
import { ButtonPrimary, FlagMessage, Link, Title } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../helpers/docs';
import { translate } from '../../helpers/l10n';
import UserForm from './components/UserForm';

interface Props {
  manageProvider?: string;
}

export default function Header(props: Props) {
  const getDocUrl = useDocUrl();
  const [openUserForm, setOpenUserForm] = React.useState(false);

  const { manageProvider } = props;
  return (
    <div>
      <div className="sw-flex sw-justify-between">
        <Title>{translate('users.page')}</Title>

        <ButtonPrimary
          id="users-create"
          disabled={manageProvider !== undefined}
          onClick={() => setOpenUserForm(true)}
        >
          {translate('users.create_user')}
        </ButtonPrimary>
      </div>
      <div>
        {manageProvider === undefined ? (
          <span>{translate('users.page.description')}</span>
        ) : (
          <FlagMessage className="sw-max-w-3/4" variant="info">
            <span>
              <FormattedMessage
                defaultMessage={translate('users.page.managed_description')}
                id="users.page.managed_description"
                values={{
                  provider: manageProvider,
                  link: (
                    <Link to={getDocUrl('/instance-administration/authentication/overview/')}>
                      {translate('documentation')}
                    </Link>
                  ),
                }}
              />
            </span>
          </FlagMessage>
        )}
      </div>

      {openUserForm && (
        <UserForm onClose={() => setOpenUserForm(false)} isInstanceManaged={false} />
      )}
    </div>
  );
}
