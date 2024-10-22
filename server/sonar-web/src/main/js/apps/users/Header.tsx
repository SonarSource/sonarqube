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
import { FormattedMessage } from 'react-intl';
import { Title } from '~design-system';
import DocumentationLink from '../../components/common/DocumentationLink';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import UserForm from './components/UserForm';

interface Props {
  manageProvider?: string;
}

export default function Header(props: Props) {
  const [openUserForm, setOpenUserForm] = React.useState(false);

  const { manageProvider } = props;
  return (
    <div>
      <div className="sw-flex sw-justify-between">
        <Title>{translate('users.page')}</Title>

        <Button
          id="users-create"
          isDisabled={manageProvider !== undefined}
          onClick={() => setOpenUserForm(true)}
          variety={ButtonVariety.Primary}
        >
          {translate('users.create_user')}
        </Button>
      </div>
      <div>
        {manageProvider === undefined ? (
          <span>{translate('users.page.description')}</span>
        ) : (
          <div className="sw-max-w-3/4 sw-mb-4">
            <FormattedMessage
              defaultMessage={translate('users.page.managed_description')}
              id="users.page.managed_description"
              values={{
                provider: translate(`managed.${manageProvider}`),
              }}
            />
            <div className="sw-mt-2">
              <FormattedMessage
                defaultMessage={translate('users.page.managed_description.recommendation')}
                id="users.page.managed_description.recommendation"
                values={{
                  link: (
                    <DocumentationLink to={DocLink.AuthOverview}>
                      {translate('users.page.managing_users')}
                    </DocumentationLink>
                  ),
                }}
              />
            </div>
          </div>
        )}
      </div>

      {openUserForm && (
        <UserForm onClose={() => setOpenUserForm(false)} isInstanceManaged={false} />
      )}
    </div>
  );
}
