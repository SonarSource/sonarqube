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
import { Spinner } from '@sonarsource/echoes-react';
import {
  Card,
  FlagMessage,
  PageContentFontWrapper,
  Title,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Image } from '~sonar-aligned/components/common/Image';
import { Location } from '~sonar-aligned/types/router';
import { translate } from '../../../helpers/l10n';
import { sanitizeUserInput } from '../../../helpers/sanitize';
import { getReturnUrl } from '../../../helpers/urls';
import { IdentityProvider } from '../../../types/types';
import LoginForm from './LoginForm';
import OAuthProviders from './OAuthProviders';

export interface LoginProps {
  identityProviders: IdentityProvider[];
  loading: boolean;
  location: Location;
  message?: string;
  onSubmit: (login: string, password: string) => Promise<void>;
}

export default function Login(props: Readonly<LoginProps>) {
  const { identityProviders, loading, location, message } = props;
  const returnTo = getReturnUrl(location);
  const displayError = Boolean(location.query.authorizationError);

  return (
    <div className="sw-flex sw-flex-col sw-items-center" id="login_form">
      <Helmet defer={false} title={translate('login.page')} />
      <Image alt="" className="sw-mt-32" src="/images/sonar-logo-horizontal.png" />
      <Card className="sw-my-14 sw-p-0 sw-w-abs-350">
        <PageContentFontWrapper className="sw-body-md sw-flex sw-flex-col sw-items-center sw-py-8 sw-px-4">
          <Image alt="" className="sw-mb-6" src="/images/embed-doc/sq-icon.svg" width={28} />
          <Title className="sw-mb-6">{translate('login.login_to_sonarqube')}</Title>
          <Spinner isLoading={loading}>
            <>
              {displayError && (
                <FlagMessage className="sw-mb-6" variant="error">
                  {translate('login.unauthorized_access_alert')}
                </FlagMessage>
              )}

              {message !== undefined && message.length > 0 && (
                <StyledMessage
                  className="markdown sw-rounded-2 sw-p-4 sw-mb-6"
                  // eslint-disable-next-line react/no-danger
                  dangerouslySetInnerHTML={{ __html: sanitizeUserInput(message) }}
                />
              )}

              {identityProviders.length > 0 && (
                <OAuthProviders identityProviders={identityProviders} returnTo={returnTo} />
              )}

              <LoginForm collapsed={identityProviders.length > 0} onSubmit={props.onSubmit} />
            </>
          </Spinner>
        </PageContentFontWrapper>
      </Card>
    </div>
  );
}

const StyledMessage = styled.div`
  background: ${themeColor('highlightedSection')};
  border: ${themeBorder('default', 'highlightedSectionBorder')};
`;
