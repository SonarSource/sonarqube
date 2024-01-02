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
import styled from '@emotion/styled';
import classNames from 'classnames';
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import IdentityProviderLink from '../../../components/controls/IdentityProviderLink';
import { translateWithParameters } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { IdentityProvider } from '../../../types/types';
import './OAuthProviders.css';

interface Props {
  className?: string;
  formatLabel?: (name: string) => React.ReactNode;
  identityProviders: IdentityProvider[];
  returnTo: string;
}

export default function OAuthProviders(props: Props) {
  const formatFunction = props.formatLabel || defaultFormatLabel;
  return (
    <Container className={classNames('oauth-providers', props.className)}>
      {props.identityProviders.map((identityProvider) => (
        <OAuthProvider
          format={formatFunction}
          identityProvider={identityProvider}
          key={identityProvider.key}
          returnTo={props.returnTo}
        />
      ))}
    </Container>
  );
}

interface ItemProps {
  format: (name: string) => React.ReactNode;
  identityProvider: IdentityProvider;
  returnTo: string;
}

function OAuthProvider({ format, identityProvider, returnTo }: ItemProps) {
  return (
    <IdentityProviderWrapper>
      <IdentityProviderLink
        backgroundColor={identityProvider.backgroundColor}
        iconPath={identityProvider.iconPath}
        name={identityProvider.name}
        url={
          `${getBaseUrl()}/sessions/init/${identityProvider.key}` +
          `?return_to=${encodeURIComponent(returnTo)}`
        }
      >
        <span>{format(identityProvider.name)}</span>
      </IdentityProviderLink>
      {identityProvider.helpMessage && (
        <HelpTooltip className="oauth-providers-help" overlay={identityProvider.helpMessage} />
      )}
    </IdentityProviderWrapper>
  );
}

function defaultFormatLabel(name: string) {
  return translateWithParameters('login.login_with_x', name);
}

const Container = styled.div`
  display: inline-flex;
  flex-direction: column;
  align-items: stretch;
`;

const IdentityProviderWrapper = styled.div`
  margin-bottom: 30px;
`;
