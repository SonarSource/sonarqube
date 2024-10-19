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

import { mockEmailConfiguration } from '../../../../../helpers/mocks/system';
import { AuthMethod, EmailConfiguration } from '../../../../../types/system';
import { checkEmailConfigurationHasChanges } from '../utils';

const validBasicValues = { basicPassword: 'password' };
const validOAuthValues = { oauthClientId: 'oauthClientId', oauthClientSecret: 'oauthClientSecret' };

describe('checkEmailConfigurationValidity empty configurations', () => {
  it('should return false if configuration is undefined', () => {
    expect(checkEmailConfigurationHasChanges(null, null)).toBe(false);
  });

  it('should return true if configuration is valid and no existing configuration', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, validBasicValues),
        null,
      ),
    ).toBe(true);
  });
});

describe('checkEmailConfigurationValidity common props', () => {
  it('should return false if authMethod is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          authMethod: undefined,
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if authMethod is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          authMethod: AuthMethod.Basic,
        }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if username is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          username: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, username: '' }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if username is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, username: 'username' }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if host is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          host: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, host: '' }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if host is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, host: 'username' }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if port is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          port: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, port: '' }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if port is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, port: 'port' }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if securityProtocol is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          securityProtocol: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          securityProtocol: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if securityProtocol is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          securityProtocol: 'securityProtocol',
        }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if fromAddress is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          fromAddress: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          fromAddress: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if fromAddress is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          fromAddress: 'fromAddress',
        }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if fromName is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          fromName: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          fromName: '',
        }) as EmailConfiguration,
        null,
      ),
    ).toBe(false);
  });

  it('should return true if fromName is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, fromName: 'fromName' }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if subjectPrefix is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          subjectPrefix: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          subjectPrefix: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if subjectPrefix is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          ...validBasicValues,
          subjectPrefix: 'subjectPrefix',
        }),
        null,
      ),
    ).toBe(true);
  });
});

describe('checkEmailConfigurationValidity basic-auth props', () => {
  it('should return false if basicPassword is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          basicPassword: undefined,
          isBasicPasswordSet: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          basicPassword: '',
          isBasicPasswordSet: undefined,
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if basicPassword is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, {
          basicPassword: 'basicPassword',
          isBasicPasswordSet: undefined,
        }),
        null,
      ),
    ).toBe(true);
  });
});

describe('checkEmailConfigurationValidity editing basic-auth props', () => {
  it('should return false if basicPassword is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { basicPassword: undefined }),
        mockEmailConfiguration(AuthMethod.Basic, validBasicValues),
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { basicPassword: '' }),
        mockEmailConfiguration(AuthMethod.Basic, validBasicValues),
      ),
    ).toBe(false);
  });

  it('should return true if basicPassword is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { basicPassword: 'basicPassword' }),
        mockEmailConfiguration(AuthMethod.Basic, validBasicValues),
      ),
    ).toBe(true);
  });
});

describe('checkEmailConfigurationValidity oauth-auth props', () => {
  it('should return false if oauthAuthenticationHost is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthAuthenticationHost: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthAuthenticationHost: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if oauthAuthenticationHost is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthAuthenticationHost: 'oauthAuthenticationHost',
        }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if oauthClientId is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientId: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientId: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if oauthClientId is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientId: 'oauthClientId',
        }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if oauthClientSecret is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientSecret: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientSecret: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if oauthClientSecret is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientSecret: 'oauthClientSecret',
        }),
        null,
      ),
    ).toBe(true);
  });

  it('should return false if oauthTenant is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthTenant: undefined,
        }),
        null,
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthTenant: '',
        }),
        null,
      ),
    ).toBe(false);
  });

  it('should return true if oauthTenant is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthTenant: 'oauthTenant',
        }),
        null,
      ),
    ).toBe(true);
  });
});

describe('checkEmailConfigurationValidity editing oauth-auth props', () => {
  it('should return false if oauthAuthenticationHost is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthAuthenticationHost: undefined,
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthAuthenticationHost: '',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(false);
  });

  it('should return true if oauthAuthenticationHost is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthAuthenticationHost: 'oauthAuthenticationHost',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(true);
  });

  it('should return true if oauthClientId is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientId: 'oauthClientId',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(true);
  });

  it('should return true if oauthClientSecret is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthClientSecret: 'oauthClientSecret',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(true);
  });

  it('should return false if oauthTenant is not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthTenant: undefined,
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(false);
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthTenant: '',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(false);
  });

  it('should return true if oauthTenant is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          ...validOAuthValues,
          oauthTenant: 'oauthTenant',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(true);
  });

  it('should return false if oauthClientId is edited in isolation', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          oauthClientId: 'oauthClientId',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(false);
  });

  it('should return true if oauthClientSecret  is edited in isolation', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          oauthClientSecret: 'oauthClientSecret',
        }),
        mockEmailConfiguration(AuthMethod.OAuth),
      ),
    ).toBe(true);
  });
});

describe('checkEmailConfigurationValidity editing', () => {
  it('should return false if configuration has not changed', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic),
        mockEmailConfiguration(AuthMethod.Basic),
      ),
    ).toBe(false);
  });

  it('should return true if configuration has changed', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, { ...validBasicValues, username: 'new-username' }),
        mockEmailConfiguration(AuthMethod.Basic),
      ),
    ).toBe(true);
  });

  it('should return false if configuration type has changed and password not set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, {
          isOauthClientIdSet: false,
          isOauthClientSecretSet: false,
        }),
        mockEmailConfiguration(AuthMethod.Basic),
      ),
    ).toBe(false);
  });

  it('should return true if configuration type has changed and password is set', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, validOAuthValues),
        mockEmailConfiguration(AuthMethod.Basic),
      ),
    ).toBe(true);
  });

  it('should return true if configuration type has changed and password already exists', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.Basic, validBasicValues),
        mockEmailConfiguration(AuthMethod.OAuth, { isBasicPasswordSet: true }),
      ),
    ).toBe(true);
  });

  it('should return true if configuration type has changed and oauthClientId & oauthClientSecret already exist', () => {
    expect(
      checkEmailConfigurationHasChanges(
        mockEmailConfiguration(AuthMethod.OAuth, { oauthClientSecret: 'oauthClientSecret' }),
        mockEmailConfiguration(AuthMethod.Basic, {
          isOauthClientIdSet: true,
          isOauthClientSecretSet: true,
        }),
      ),
    ).toBe(true);
  });
});
