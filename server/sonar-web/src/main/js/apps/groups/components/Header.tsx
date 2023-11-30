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
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import { Button } from '../../../components/controls/buttons';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { Provider } from '../../../types/types';
import GroupForm from './GroupForm';

interface HeaderProps {
  manageProvider: Provider | undefined;
}

export default function Header({ manageProvider }: Readonly<HeaderProps>) {
  const [createModal, setCreateModal] = React.useState(false);

  return (
    <>
      <div className="page-header null-spacer-bottom" id="groups-header">
        <h2 className="page-title">{translate('user_groups.page')}</h2>

        <div className="page-actions">
          <Button
            id="groups-create"
            disabled={manageProvider !== undefined}
            onClick={() => setCreateModal(true)}
          >
            {translate('groups.create_group')}
          </Button>
        </div>

        {manageProvider === undefined ? (
          <p className="page-description">{translate('user_groups.page.description')}</p>
        ) : (
          <Alert className="page-description max-width-100 width-100" variant="info">
            <FormattedMessage
              defaultMessage={translate('user_groups.page.managed_description')}
              id="user_groups.page.managed_description"
              values={{
                provider: manageProvider,
                link: (
                  <DocLink to="/instance-administration/authentication/overview/">
                    {translate('documentation')}
                  </DocLink>
                ),
              }}
            />
          </Alert>
        )}
      </div>
      {createModal && <GroupForm onClose={() => setCreateModal(false)} create />}
    </>
  );
}
