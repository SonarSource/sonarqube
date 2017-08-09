/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { translateWithParameters } from '../../../helpers/l10n';

/*::
type Props = {
  formatLabel?: string => string,
  identityProviders: Array<{
    backgroundColor: string,
    iconPath: string,
    key: string,
    name: string
  }>
};
*/

export default function OAuthProviders(props /*: Props */) {
  return (
    <section className="oauth-providers">
      <ul>
        {props.identityProviders.map(identityProvider =>
          <li key={identityProvider.key}>
            <a
              href={`${window.baseUrl}/sessions/init/${identityProvider.key}`}
              style={{ backgroundColor: identityProvider.backgroundColor }}
              // $FlowFixMe formatLabel is always defined through defaultProps
              title={props.formatLabel(identityProvider.name)}>
              <img
                alt={identityProvider.name}
                width="20"
                height="20"
                src={window.baseUrl + identityProvider.iconPath}
              />
              <span>
                {/* $FlowFixMe formatLabel is always defined through defaultProps */}
                {props.formatLabel(identityProvider.name)}
              </span>
            </a>
          </li>
        )}
      </ul>
    </section>
  );
}

OAuthProviders.defaultProps = {
  formatLabel: (name /*: string */) => translateWithParameters('login.login_with_x', name)
};
