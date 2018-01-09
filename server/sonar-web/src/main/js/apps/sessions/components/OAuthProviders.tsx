/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { translateWithParameters } from '../../../helpers/l10n';
import { IdentityProvider } from '../../../api/users';
import { getBaseUrl } from '../../../helpers/urls';
import './OAuthProviders.css';

interface Props {
  formatLabel?: (name: string) => string;
  identityProviders: IdentityProvider[];
  returnTo: string;
}

export default function OAuthProviders(props: Props) {
  const formatLabel = props.formatLabel || defaultFormatLabel;
  return (
    <section className="oauth-providers">
      <ul>
        {props.identityProviders.map(identityProvider => (
          <li key={identityProvider.key}>
            <a
              href={
                `${getBaseUrl()}/sessions/init/${identityProvider.key}` +
                `?return_to=${encodeURIComponent(props.returnTo)}`
              }
              style={{ backgroundColor: identityProvider.backgroundColor }}
              title={formatLabel(identityProvider.name)}>
              <img
                alt={identityProvider.name}
                width="20"
                height="20"
                src={getBaseUrl() + identityProvider.iconPath}
              />
              <span>{formatLabel(identityProvider.name)}</span>
            </a>
          </li>
        ))}
      </ul>
    </section>
  );
}

function defaultFormatLabel(name: string) {
  return translateWithParameters('login.login_with_x', name);
}
