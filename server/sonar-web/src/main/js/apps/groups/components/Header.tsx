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
import { ButtonPrimary, FlagMessage, Title } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
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
      <div id="groups-header">
        <div className="sw-flex sw-justify-between">
          <Title className="sw-mb-4">{translate('user_groups.page')}</Title>
          <ButtonPrimary
            id="groups-create"
            disabled={manageProvider !== undefined}
            onClick={() => setCreateModal(true)}
          >
            {translate('groups.create_group')}
          </ButtonPrimary>
        </div>

        {manageProvider === undefined ? (
          <p className="sw-mb-4">{translate('user_groups.page.description')}</p>
        ) : (
          <FlagMessage className="sw-mb-4 sw-max-w-full sw-w-full" variant="info">
            <div>
              <FormattedMessage
                defaultMessage={translate('user_groups.page.managed_description')}
                id="user_groups.page.managed_description"
                values={{
                  provider: manageProvider,
                  link: (
                    <DocumentationLink to="/instance-administration/authentication/overview/">
                      {translate('documentation')}
                    </DocumentationLink>
                  ),
                }}
              />
            </div>
          </FlagMessage>
        )}
      </div>
      {createModal && <GroupForm onClose={() => setCreateModal(false)} create />}
    </>
  );
}
