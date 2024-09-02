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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { addGlobalSuccessMessage } from 'design-system/lib';
import React from 'react';
import { byLabelText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import * as api from '../../../../../api/system';
import { mockEmailConfiguration } from '../../../../../helpers/mocks/system';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { AuthMethod } from '../../../../../types/system';
import EmailNotification from '../EmailNotification';

jest.mock('../../../../../api/system');
jest.mock('../../../../../api/settings');

jest.mock('design-system', () => ({
  ...jest.requireActual('design-system'),
  addGlobalSuccessMessage: jest.fn(),
}));

const systemHandler = new SystemServiceMock();

beforeEach(() => {
  jest.clearAllMocks();
  systemHandler.reset();
});

const ui = {
  editSubheading1: byText('email_notification.subheading.1'),

  // common fields
  selectorBasicAuth: byRole('radio', {
    name: 'email_notification.form.basic_auth.title email_notification.form.basic_auth.description',
  }),
  selectorOAuthAuth: byRole('radio', {
    name: 'email_notification.form.oauth_auth.title email_notification.form.oauth_auth.description email_notification.form.oauth_auth.supported recommended email_notification.form.oauth_auth.recommended_reason',
  }),
  host: byRole('textbox', {
    name: 'email_notification.form.host field_required',
  }),
  port: byRole('spinbutton', {
    name: 'email_notification.form.port field_required',
  }),
  securityProtocol: byRole('searchbox', {
    name: 'email_notification.form.security_protocol field_required',
  }),
  fromAddress: byRole('textbox', {
    name: 'email_notification.form.from_address field_required',
  }),
  fromName: byRole('textbox', {
    name: 'email_notification.form.from_name field_required',
  }),
  subjectPrefix: byRole('textbox', {
    name: 'email_notification.form.subject_prefix field_required',
  }),
  username: byRole('textbox', {
    name: 'email_notification.form.username field_required',
  }),

  // basic authentication
  basic_password: byLabelText('email_notification.form.basic_password*'),

  // oauth
  oauth_auth_host: byRole('textbox', {
    name: 'email_notification.form.oauth_authentication_host field_required',
  }),
  oauth_client_id: byLabelText('email_notification.form.oauth_client_id*'),
  oauth_client_id_edit: byTestId('email_notification.form.oauth_client_id-edit'),
  oauth_client_id_reset: byTestId('email_notification.form.oauth_client_id-reset'),
  oauth_client_secret: byLabelText('email_notification.form.oauth_client_secret*'),
  oauth_tenant: byRole('textbox', { name: 'email_notification.form.oauth_tenant field_required' }),

  save: byRole('button', {
    name: 'email_notification.form.save_configuration',
  }),

  // overview values
  overviewHeading: byText('email_notification.overview.heading'),
  overview_auth_mode: byTestId('email_notification.overview.authentication_type.value'),
  overview_username: byTestId('email_notification.form.username.value'),
  overview_basic_password: byTestId('email_notification.form.basic_password.value'),
  overview_oauth_auth_host: byTestId('email_notification.form.oauth_authentication_host.value'),
  overview_oauth_client_id: byTestId('email_notification.form.oauth_client_id.value'),
  overview_oauth_client_secret: byTestId('email_notification.form.oauth_client_secret.value'),
  overview_oauth_tenant: byTestId('email_notification.form.oauth_tenant.value'),
  overview_host: byTestId('email_notification.form.host.value'),
  overview_port: byTestId('email_notification.form.port.value'),
  overview_security_protocol: byTestId('email_notification.form.security_protocol.value'),
  overview_from_address: byTestId('email_notification.form.from_address.value'),
  overview_from_name: byTestId('email_notification.form.from_name.value'),
  overview_subject_prefix: byTestId('email_notification.form.subject_prefix.value'),

  edit: byRole('button', {
    name: 'edit',
  }),
};

describe('Email Basic Configuration', () => {
  it('can save the basic configuration', async () => {
    jest.spyOn(api, 'postEmailConfiguration');
    const user = userEvent.setup();
    renderEmailNotifications();
    expect(await ui.editSubheading1.find()).toBeInTheDocument();

    expect(ui.save.get()).toBeDisabled();

    expect(ui.selectorBasicAuth.get()).toBeChecked();
    expect(ui.username.get()).toHaveValue('');
    expect(ui.basic_password.get()).toHaveValue('');
    expect(ui.host.get()).toHaveValue('');
    expect(ui.port.get()).toHaveValue(587);
    expect(ui.securityProtocol.get()).toHaveValue('');
    expect(ui.fromAddress.get()).toHaveValue('');
    expect(ui.fromName.get()).toHaveValue('SonarQube');
    expect(ui.subjectPrefix.get()).toHaveValue('[SonarQube]');

    await user.type(ui.basic_password.get(), 'password');
    await user.type(ui.host.get(), 'host');
    await user.clear(ui.port.get());
    await user.type(ui.port.get(), '1234');
    await user.click(ui.securityProtocol.get());
    await user.click(screen.getByText('SSLTLS'));
    await user.type(ui.fromAddress.get(), 'admin@localhost.com');
    await user.clear(ui.fromName.get());
    await user.type(ui.fromName.get(), 'fromName');
    await user.clear(ui.subjectPrefix.get());
    await user.type(ui.subjectPrefix.get(), 'prefix');
    await user.type(ui.username.get(), 'username');

    expect(ui.selectorBasicAuth.get()).toBeChecked();
    expect(ui.username.get()).toHaveValue('username');
    expect(ui.basic_password.get()).toHaveValue('password');
    expect(ui.host.get()).toHaveValue('host');
    expect(ui.port.get()).toHaveValue(1234);
    expect(ui.securityProtocol.get()).toHaveValue('SSLTLS');
    expect(ui.fromAddress.get()).toHaveValue('admin@localhost.com');
    expect(ui.fromName.get()).toHaveValue('fromName');
    expect(ui.subjectPrefix.get()).toHaveValue('prefix');

    expect(await ui.save.find()).toBeEnabled();
    await user.click(ui.save.get());

    expect(api.postEmailConfiguration).toHaveBeenCalledTimes(1);
    expect(api.postEmailConfiguration).toHaveBeenCalledWith({
      authMethod: 'BASIC',
      basicPassword: 'password',
      fromAddress: 'admin@localhost.com',
      fromName: 'fromName',
      host: 'host',
      port: '1234',
      securityProtocol: 'SSLTLS',
      subjectPrefix: 'prefix',
      username: 'username',
    });

    expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
      'email_notification.form.save_configuration.create_success',
    );

    expect(await ui.overviewHeading.find()).toBeInTheDocument();

    expect(ui.overview_auth_mode.get()).toHaveTextContent(AuthMethod.Basic);
    expect(ui.overview_username.get()).toHaveTextContent('username');
    expect(ui.overview_basic_password.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_host.get()).toHaveTextContent('host');
    expect(ui.overview_port.get()).toHaveTextContent('1234');
    expect(ui.overview_security_protocol.get()).toHaveTextContent('SSLTLS');
    expect(ui.overview_from_address.get()).toHaveTextContent('admin@localhost.com');
    expect(ui.overview_from_name.get()).toHaveTextContent('fromName');
    expect(ui.overview_subject_prefix.get()).toHaveTextContent('prefix');
  });

  it('renders the overview after loading configuration', async () => {
    systemHandler.addEmailConfiguration(
      mockEmailConfiguration(AuthMethod.Basic, { id: 'email-1' }),
    );

    renderEmailNotifications();

    expect(await ui.overviewHeading.find()).toBeInTheDocument();
    expect(ui.overview_auth_mode.get()).toHaveTextContent(AuthMethod.Basic);
    expect(ui.overview_username.get()).toHaveTextContent('username');
    expect(ui.overview_basic_password.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_host.get()).toHaveTextContent('host');
    expect(ui.overview_port.get()).toHaveTextContent('port');
    expect(ui.overview_security_protocol.get()).toHaveTextContent('SSLTLS');
    expect(ui.overview_from_address.get()).toHaveTextContent('from_address');
    expect(ui.overview_from_name.get()).toHaveTextContent('from_name');
    expect(ui.overview_subject_prefix.get()).toHaveTextContent('subject_prefix');
  });

  it('can edit an existing configuration', async () => {
    systemHandler.addEmailConfiguration(
      mockEmailConfiguration(AuthMethod.Basic, { id: 'email-1' }),
    );
    jest.spyOn(api, 'patchEmailConfiguration');
    const user = userEvent.setup();
    renderEmailNotifications();

    expect(await ui.overviewHeading.find()).toBeInTheDocument();

    await user.click(ui.edit.get());

    expect(await ui.editSubheading1.find()).toBeInTheDocument();

    expect(ui.save.get()).toBeDisabled();
    await user.type(ui.basic_password.get(), 'updated');
    await user.type(ui.host.get(), '-updated');
    await user.type(ui.port.get(), '5678');
    await user.click(ui.securityProtocol.get());
    await user.click(screen.getByText('STARTTLS'));
    await user.clear(ui.fromAddress.get());
    await user.type(ui.fromAddress.get(), 'updated@email.com');
    await user.type(ui.fromName.get(), '-updated');
    await user.type(ui.subjectPrefix.get(), '-updated');
    await user.type(ui.username.get(), '-updated');

    expect(await ui.save.find()).toBeEnabled();
    await user.click(ui.save.get());

    expect(api.patchEmailConfiguration).toHaveBeenCalledTimes(1);
    expect(api.patchEmailConfiguration).toHaveBeenCalledWith('email-1', {
      authMethod: 'BASIC',
      basicPassword: 'updated',
      fromAddress: 'updated@email.com',
      fromName: 'from_name-updated',
      host: 'host-updated',
      id: 'email-1',
      isBasicPasswordSet: true,
      port: '5678',
      securityProtocol: 'STARTTLS',
      subjectPrefix: 'subject_prefix-updated',
      username: 'username-updated',
    });

    expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
      'email_notification.form.save_configuration.update_success',
    );

    expect(await ui.overviewHeading.find()).toBeInTheDocument();

    expect(ui.overview_auth_mode.get()).toHaveTextContent(AuthMethod.Basic);
    expect(ui.overview_username.get()).toHaveTextContent('username-updated');
    expect(ui.overview_basic_password.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_host.get()).toHaveTextContent('host-updated');
    expect(ui.overview_port.get()).toHaveTextContent('5678');
    expect(ui.overview_security_protocol.get()).toHaveTextContent('STARTTLS');
    expect(ui.overview_from_address.get()).toHaveTextContent('updated@email.com');
    expect(ui.overview_from_name.get()).toHaveTextContent('from_name-updated');
    expect(ui.overview_subject_prefix.get()).toHaveTextContent('subject_prefix-updated');
  });
});

describe('Email Oauth Configuration', () => {
  it('can save the oauth configuration', async () => {
    jest.spyOn(api, 'postEmailConfiguration');
    const user = userEvent.setup();
    renderEmailNotifications();
    expect(await ui.editSubheading1.find()).toBeInTheDocument();
    await user.click(ui.selectorOAuthAuth.get());

    expect(ui.save.get()).toBeDisabled();

    expect(ui.selectorOAuthAuth.get()).toBeChecked();
    expect(ui.oauth_auth_host.get()).toHaveValue('');
    expect(ui.oauth_client_id.get()).toHaveValue('');
    expect(ui.oauth_client_secret.get()).toHaveValue('');
    expect(ui.oauth_tenant.get()).toHaveValue('');
    expect(ui.host.get()).toHaveValue('');
    expect(ui.port.get()).toHaveValue(587);
    expect(ui.securityProtocol.get()).toHaveValue('');
    expect(ui.fromAddress.get()).toHaveValue('');
    expect(ui.fromName.get()).toHaveValue('SonarQube');
    expect(ui.subjectPrefix.get()).toHaveValue('[SonarQube]');

    await user.type(ui.oauth_auth_host.get(), 'oauth_auth_host');
    await user.type(ui.oauth_client_id.get(), 'oauth_client_id');
    await user.type(ui.oauth_client_secret.get(), 'oauth_client_secret');
    await user.type(ui.oauth_tenant.get(), 'oauth_tenant');
    await user.type(ui.host.get(), 'host');
    await user.clear(ui.port.get());
    await user.type(ui.port.get(), '1234');
    await user.click(ui.securityProtocol.get());
    await user.click(screen.getByText('SSLTLS'));
    await user.type(ui.fromAddress.get(), 'admin@localhost.com');
    await user.clear(ui.fromName.get());
    await user.type(ui.fromName.get(), 'fromName');
    await user.clear(ui.subjectPrefix.get());
    await user.type(ui.subjectPrefix.get(), 'prefix');
    await user.type(ui.username.get(), 'username');

    expect(ui.selectorOAuthAuth.get()).toBeChecked();
    expect(ui.username.get()).toHaveValue('username');
    expect(ui.oauth_auth_host.get()).toHaveValue('oauth_auth_host');
    expect(ui.oauth_client_id.get()).toHaveValue('oauth_client_id');
    expect(ui.oauth_client_secret.get()).toHaveValue('oauth_client_secret');
    expect(ui.oauth_tenant.get()).toHaveValue('oauth_tenant');
    expect(ui.host.get()).toHaveValue('host');
    expect(ui.port.get()).toHaveValue(1234);
    expect(ui.securityProtocol.get()).toHaveValue('SSLTLS');
    expect(ui.fromAddress.get()).toHaveValue('admin@localhost.com');
    expect(ui.fromName.get()).toHaveValue('fromName');
    expect(ui.subjectPrefix.get()).toHaveValue('prefix');

    expect(await ui.save.find()).toBeEnabled();
    await user.click(ui.save.get());

    expect(api.postEmailConfiguration).toHaveBeenCalledTimes(1);
    expect(api.postEmailConfiguration).toHaveBeenCalledWith({
      authMethod: 'OAUTH',
      basicPassword: '',
      oauthAuthenticationHost: 'oauth_auth_host',
      oauthClientId: 'oauth_client_id',
      oauthClientSecret: 'oauth_client_secret',
      oauthTenant: 'oauth_tenant',
      fromAddress: 'admin@localhost.com',
      fromName: 'fromName',
      host: 'host',
      port: '1234',
      securityProtocol: 'SSLTLS',
      subjectPrefix: 'prefix',
      username: 'username',
    });

    expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
      'email_notification.form.save_configuration.create_success',
    );

    expect(await ui.overviewHeading.find()).toBeInTheDocument();

    expect(ui.overview_auth_mode.get()).toHaveTextContent(AuthMethod.OAuth);
    expect(ui.overview_oauth_auth_host.get()).toHaveTextContent('oauth_auth_host');
    expect(ui.overview_oauth_client_id.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_oauth_client_secret.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_oauth_tenant.get()).toHaveTextContent('oauth_tenant');
    expect(ui.overview_host.get()).toHaveTextContent('host');
    expect(ui.overview_port.get()).toHaveTextContent('1234');
    expect(ui.overview_security_protocol.get()).toHaveTextContent('SSLTLS');
    expect(ui.overview_from_address.get()).toHaveTextContent('admin@localhost.com');
    expect(ui.overview_from_name.get()).toHaveTextContent('fromName');
    expect(ui.overview_subject_prefix.get()).toHaveTextContent('prefix');
    expect(ui.overview_username.get()).toHaveTextContent('username');
  });

  it('renders the overview after loading configuration', async () => {
    systemHandler.addEmailConfiguration(
      mockEmailConfiguration(AuthMethod.OAuth, { id: 'email-2' }),
    );

    renderEmailNotifications();

    expect(await ui.overviewHeading.find()).toBeInTheDocument();
    expect(ui.overview_auth_mode.get()).toHaveTextContent(AuthMethod.OAuth);
    expect(ui.overview_oauth_auth_host.get()).toHaveTextContent('oauth_auth_host');
    expect(ui.overview_oauth_client_id.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_oauth_client_secret.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_oauth_tenant.get()).toHaveTextContent('oauth_tenant');
    expect(ui.overview_host.get()).toHaveTextContent('host');
    expect(ui.overview_port.get()).toHaveTextContent('port');
    expect(ui.overview_security_protocol.get()).toHaveTextContent('SSLTLS');
    expect(ui.overview_from_address.get()).toHaveTextContent('from_address');
    expect(ui.overview_from_name.get()).toHaveTextContent('from_name');
    expect(ui.overview_subject_prefix.get()).toHaveTextContent('subject_prefix');
  });

  it('can edit the configuration', async () => {
    systemHandler.addEmailConfiguration(
      mockEmailConfiguration(AuthMethod.OAuth, { id: 'email-1' }),
    );
    jest.spyOn(api, 'patchEmailConfiguration');
    const user = userEvent.setup();
    renderEmailNotifications();

    expect(await ui.overviewHeading.find()).toBeInTheDocument();
    await user.click(ui.edit.get());

    expect(await ui.editSubheading1.find()).toBeInTheDocument();
    await user.click(ui.selectorOAuthAuth.get());

    expect(ui.save.get()).toBeDisabled();

    await user.type(ui.oauth_auth_host.get(), '-updated');
    await user.click(ui.oauth_client_id_edit.get());
    await user.type(ui.oauth_client_id.get(), 'updated_id');
    await user.type(ui.oauth_client_secret.get(), 'updated_secret');
    await user.type(ui.oauth_tenant.get(), '-updated');
    await user.type(ui.host.get(), '-updated');
    await user.type(ui.port.get(), '5678');
    await user.click(ui.securityProtocol.get());
    await user.click(screen.getByText('STARTTLS'));
    await user.clear(ui.fromAddress.get());
    await user.type(ui.fromAddress.get(), 'updated@email.com');
    await user.type(ui.fromName.get(), '-updated');
    await user.type(ui.subjectPrefix.get(), '-updated');
    await user.type(ui.username.get(), '-updated');

    expect(await ui.save.find()).toBeEnabled();
    await user.click(ui.save.get());

    expect(api.patchEmailConfiguration).toHaveBeenCalledTimes(1);
    expect(api.patchEmailConfiguration).toHaveBeenCalledWith('email-1', {
      authMethod: 'OAUTH',
      oauthAuthenticationHost: 'oauth_auth_host-updated',
      oauthClientId: 'updated_id',
      oauthClientSecret: 'updated_secret',
      oauthTenant: 'oauth_tenant-updated',
      fromAddress: 'updated@email.com',
      fromName: 'from_name-updated',
      host: 'host-updated',
      id: 'email-1',
      isOauthClientIdSet: true,
      isOauthClientSecretSet: true,
      port: '5678',
      securityProtocol: 'STARTTLS',
      subjectPrefix: 'subject_prefix-updated',
      username: 'username-updated',
    });

    expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
      'email_notification.form.save_configuration.update_success',
    );

    expect(await ui.overviewHeading.find()).toBeInTheDocument();

    expect(ui.overview_auth_mode.get()).toHaveTextContent(AuthMethod.OAuth);
    expect(ui.overview_oauth_auth_host.get()).toHaveTextContent('oauth_auth_host');
    expect(ui.overview_oauth_client_id.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_oauth_client_secret.get()).toHaveTextContent(
      'email_notification.overview.private',
    );
    expect(ui.overview_oauth_tenant.get()).toHaveTextContent('oauth_tenant');
    expect(ui.overview_host.get()).toHaveTextContent('host');
    expect(ui.overview_port.get()).toHaveTextContent('5678');
    expect(ui.overview_security_protocol.get()).toHaveTextContent('STARTTLS');
    expect(ui.overview_from_address.get()).toHaveTextContent('updated@email.com');
    expect(ui.overview_from_name.get()).toHaveTextContent('from_name-updated');
    expect(ui.overview_subject_prefix.get()).toHaveTextContent('subject_prefix-updated');
    expect(ui.overview_username.get()).toHaveTextContent('username-updated');
  });
});

function renderEmailNotifications() {
  return renderComponent(<EmailNotification />);
}
