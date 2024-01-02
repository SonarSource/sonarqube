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

import { Badge, Note, getTextColor } from 'design-system';
import * as React from 'react';
import { colors } from '../../../app/theme';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { IdentityProvider, Provider } from '../../../types/types';
import { RestUserDetailed } from '../../../types/users';

export interface Props {
  identityProvider?: IdentityProvider;
  user: RestUserDetailed;
  manageProvider: Provider | undefined;
}

export default function UserListItemIdentity({ identityProvider, user, manageProvider }: Props) {
  return (
    <div>
      <div className="sw-flex sw-flex-col">
        <strong className="it__user-name sw-body-sm-highlight">{user.name}</strong>
        <Note className="it__user-login">{user.login}</Note>
      </div>
      {user.email && <div className="it__user-email sw-mt-1">{user.email}</div>}
      {!user.local && user.externalProvider !== 'sonarqube' && (
        <ExternalProvider identityProvider={identityProvider} user={user} />
      )}
      {!user.managed && manageProvider !== undefined && <Badge>{translate('local')}</Badge>}
    </div>
  );
}

export function ExternalProvider({ identityProvider, user }: Omit<Props, 'manageProvider'>) {
  if (!identityProvider) {
    return (
      <div className="it__user-identity-provider sw-mt-1">
        <span>
          {user.externalProvider}: {user.externalLogin}
        </span>
      </div>
    );
  }

  return (
    <div className="it__user-identity-provider sw-mt-1">
      <div
        style={{
          backgroundColor: identityProvider.backgroundColor,
          color: getTextColor(identityProvider.backgroundColor, colors.secondFontColor),
        }}
      >
        <img
          alt={identityProvider.name}
          className="sw-mr-1"
          height="14"
          src={getBaseUrl() + identityProvider.iconPath}
          width="14"
        />
        {user.externalLogin}
      </div>
    </div>
  );
}
