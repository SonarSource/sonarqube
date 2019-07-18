/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import IdentityProviderLink from 'sonar-ui-common/components/controls/IdentityProviderLink';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import './OAuthProviders.css';

interface Props {
  className?: string;
  formatLabel?: (name: string) => React.ReactNode;
  identityProviders: T.IdentityProvider[];
  returnTo: string;
}

export default function OAuthProviders(props: Props) {
  const formatFunction = props.formatLabel || defaultFormatLabel;
  return (
    <section className={classNames('oauth-providers', props.className)}>
      <ul>
        {props.identityProviders.map(identityProvider => (
          <OAuthProvider
            format={formatFunction}
            identityProvider={identityProvider}
            key={identityProvider.key}
            returnTo={props.returnTo}
          />
        ))}
      </ul>
    </section>
  );
}

interface ItemProps {
  format: (name: string) => React.ReactNode;
  identityProvider: T.IdentityProvider;
  returnTo: string;
}

function OAuthProvider({ format, identityProvider, returnTo }: ItemProps) {
  return (
    <li>
      <IdentityProviderLink
        backgroundColor={identityProvider.backgroundColor}
        iconPath={identityProvider.iconPath}
        name={identityProvider.name}
        url={
          `${getBaseUrl()}/sessions/init/${identityProvider.key}` +
          `?return_to=${encodeURIComponent(returnTo)}`
        }>
        <span>{format(identityProvider.name)}</span>
      </IdentityProviderLink>
      {identityProvider.helpMessage && (
        <HelpTooltip className="oauth-providers-help" overlay={identityProvider.helpMessage} />
      )}
    </li>
  );
}

function defaultFormatLabel(name: string) {
  return translateWithParameters('login.login_with_x', name);
}
