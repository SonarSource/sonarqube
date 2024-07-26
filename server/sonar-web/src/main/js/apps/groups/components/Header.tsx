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
import { Title } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
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
          <Button
            id="groups-create"
            isDisabled={manageProvider !== undefined}
            onClick={() => setCreateModal(true)}
            variety={ButtonVariety.Primary}
          >
            {translate('groups.create_group')}
          </Button>
        </div>

        {manageProvider === undefined ? (
          <p className="sw-mb-4">{translate('user_groups.page.description')}</p>
        ) : (
          <div className="sw-max-w-3/4 sw-mb-4">
            <FormattedMessage
              defaultMessage={translate('user_groups.page.managed_description')}
              id="user_groups.page.managed_description"
              values={{
                provider: translate(`managed.${manageProvider}`),
              }}
            />
            <div className="sw-mt-2">
              <FormattedMessage
                defaultMessage={translate('user_groups.page.managed_description2')}
                id="user_groups.page.managed_description2"
                values={{
                  link: (
                    <DocumentationLink to={DocLink.AuthOverview}>
                      {translate('user_groups.page.managing_groups')}
                    </DocumentationLink>
                  ),
                }}
              />
            </div>
          </div>
        )}
      </div>
      {createModal && <GroupForm onClose={() => setCreateModal(false)} create />}
    </>
  );
}
