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
import { isUndefined } from 'lodash';
import { AuthMethod, EmailConfiguration } from '../../../../types/system';

export const AUTH_METHOD = 'auth-method';

// Basic Authentication
export const BASIC_PASSWORD = 'basic-password';
export const IS_BASIC_PASSWORD_SET = 'is-basic-password-set';

// OAuth Authentication
export const IS_OAUTH_CLIENT_ID_SET = 'is-oauth-client-id-set';
export const IS_OAUTH_CLIENT_SECRET_SET = 'is-oauth-client-secret-set';
export const OAUTH_AUTHENTICATION_HOST = 'oauth-authentication-host';
export const OAUTH_CLIENT_ID = 'oauth-client-id';
export const OAUTH_CLIENT_SECRET = 'oauth-client-secret';
export const OAUTH_TENANT = 'oauth-tenant';

// Common settings
export const USERNAME = 'username';
export const HOST = 'host';
export const PORT = 'port';
export const SECURITY_PROTOCOL = 'security-protocol';
export const FROM_ADDRESS = 'from-address';
export const FROM_NAME = 'from-mame';
export const SUBJECT_PREFIX = 'subject-prefix';

export interface EmailNotificationGroupProps {
  configuration: EmailConfiguration;
  onChange: (newValue: Partial<EmailConfiguration>) => void;
}

export function checkEmailConfigurationValidity(configuration: EmailConfiguration): boolean {
  let isValid = false;
  const commonProps: (keyof EmailConfiguration)[] = [
    'authMethod',
    'username',
    'host',
    'port',
    'securityProtocol',
    'fromAddress',
    'fromName',
    'subjectPrefix',
  ];

  if (configuration.authMethod === AuthMethod.Basic) {
    isValid = checkRequiredPropsAreValid(configuration, [...commonProps, 'basicPassword']);
  } else {
    isValid = checkRequiredPropsAreValid(configuration, [
      ...commonProps,
      'oauthAuthenticationHost',
      'oauthClientId',
      'oauthClientSecret',
      'oauthTenant',
    ]);
  }

  return isValid;
}

function checkRequiredPropsAreValid<T>(obj: T, props: (keyof T)[]): boolean {
  return props.every(
    (prop) => !isUndefined(obj[prop]) && typeof obj[prop] === 'string' && obj[prop].length > 0,
  );
}
