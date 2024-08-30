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

import { AuthMethod, EmailConfiguration } from '../../types/system';

export function mockEmailConfiguration(
  authMethod: AuthMethod,
  overrides: Partial<EmailConfiguration> = {},
): EmailConfiguration {
  const base: Partial<EmailConfiguration> = {
    fromAddress: 'from_address',
    fromName: 'from_name',
    host: 'host',
    id: '1',
    port: 'port',
    subjectPrefix: 'subject_prefix',
    securityProtocol: 'SSLTLS',
    username: 'username',
  };

  const mock =
    authMethod === AuthMethod.Basic
      ? {
          ...base,
          authMethod: AuthMethod.Basic,
          basicPassword: undefined,
          isBasicPasswordSet: true,
        }
      : {
          ...base,
          authMethod: AuthMethod.OAuth,
          isOauthClientIdSet: true,
          isOauthClientSecretSet: true,
          oauthAuthenticationHost: 'oauth_auth_host',
          oauthClientId: undefined,
          oauthClientSecret: undefined,
          oauthTenant: 'oauth_tenant',
        };

  return { ...mock, ...overrides } as EmailConfiguration;
}
