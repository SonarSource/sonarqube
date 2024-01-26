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
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import React from 'react';
import ScimProvisioningServiceMock from '../../../../../api/mocks/ScimProvisioningServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../../helpers/testSelector';
import { Feature } from '../../../../../types/features';
import Authentication from '../Authentication';

let handler: ScimProvisioningServiceMock;
let system: SystemServiceMock;
let settingsHandler: SettingsServiceMock;

beforeEach(() => {
  handler = new ScimProvisioningServiceMock();
  system = new SystemServiceMock();
  settingsHandler = new SettingsServiceMock();
  [
    {
      key: 'sonar.auth.saml.signature.enabled',
      value: 'false',
    },
    {
      key: 'sonar.auth.saml.enabled',
      value: 'false',
    },
    {
      key: 'sonar.auth.saml.applicationId',
      value: 'sonarqube',
    },
    {
      key: 'sonar.auth.saml.providerName',
      value: 'SAML',
    },
  ].forEach((setting: any) => settingsHandler.set(setting.key, setting.value));
});

afterEach(() => {
  handler.reset();
  settingsHandler.reset();
  system.reset();
});

const samlContainer = byRole('tabpanel', { name: 'SAML' });

const ui = {
  noSamlConfiguration: byText('settings.authentication.saml.form.not_configured'),
  createConfigButton: samlContainer.byRole('button', {
    name: 'settings.authentication.form.create',
  }),
  providerName: byRole('textbox', { name: 'property.sonar.auth.saml.providerName.name' }),
  providerId: byRole('textbox', { name: 'property.sonar.auth.saml.providerId.name' }),
  providerCertificate: byRole('textbox', {
    name: 'property.sonar.auth.saml.certificate.secured.name',
  }),
  loginUrl: byRole('textbox', { name: 'property.sonar.auth.saml.loginUrl.name' }),
  userLoginAttribute: byRole('textbox', { name: 'property.sonar.auth.saml.user.login.name' }),
  userNameAttribute: byRole('textbox', { name: 'property.sonar.auth.saml.user.name.name' }),
  saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
  confirmProvisioningButton: byRole('button', {
    name: 'yes',
  }),
  saveScim: samlContainer.byRole('button', { name: 'save' }),
  enableConfigButton: samlContainer.byRole('button', {
    name: 'settings.authentication.form.enable',
  }),
  disableConfigButton: samlContainer.byRole('button', {
    name: 'settings.authentication.form.disable',
  }),
  editConfigButton: samlContainer.byRole('button', {
    name: 'settings.authentication.form.edit',
  }),
  enableFirstMessage: byText('settings.authentication.saml.enable_first'),
  jitProvisioningButton: byRole('radio', {
    name: /settings.authentication.saml.form.provisioning_at_login/,
  }),
  scimProvisioningButton: byRole('radio', {
    name: /settings.authentication.saml.form.provisioning_with_scim/,
  }),
  fillForm: async (user: UserEvent) => {
    await user.clear(ui.providerName.get());
    await user.type(ui.providerName.get(), 'Awsome SAML config');
    await user.type(ui.providerId.get(), 'okta-1234');
    await user.type(ui.loginUrl.get(), 'http://test.org');
    await user.type(ui.providerCertificate.get(), '-secret-');
    await user.type(ui.userLoginAttribute.get(), 'login');
    await user.type(ui.userNameAttribute.get(), 'name');
  },
  createConfiguration: async (user: UserEvent) => {
    await user.click(await ui.createConfigButton.find());
    await ui.fillForm(user);
    await user.click(ui.saveConfigButton.get());
  },
};

it('should render an empty SAML configuration', async () => {
  renderAuthentication();
  expect(await ui.noSamlConfiguration.find()).toBeInTheDocument();
});

it('should be able to create a configuration', async () => {
  const user = userEvent.setup();
  renderAuthentication();

  await user.click(await ui.createConfigButton.find());

  expect(ui.saveConfigButton.get()).toBeDisabled();
  await ui.fillForm(user);
  expect(ui.saveConfigButton.get()).toBeEnabled();

  await user.click(ui.saveConfigButton.get());

  expect(await ui.editConfigButton.find()).toBeInTheDocument();
});

it('should be able to enable/disable configuration', async () => {
  const user = userEvent.setup();
  renderAuthentication();

  await ui.createConfiguration(user);
  await user.click(await ui.enableConfigButton.find());

  expect(await ui.disableConfigButton.find()).toBeInTheDocument();
  await user.click(ui.disableConfigButton.get());
  await waitFor(() => expect(ui.disableConfigButton.query()).not.toBeInTheDocument());

  expect(await ui.enableConfigButton.find()).toBeInTheDocument();
});

it('should be able to choose provisioning', async () => {
  const user = userEvent.setup();

  renderAuthentication([Feature.Scim]);

  await ui.createConfiguration(user);

  expect(await ui.enableFirstMessage.find()).toBeInTheDocument();
  await user.click(await ui.enableConfigButton.find());

  expect(await ui.jitProvisioningButton.find()).toBeChecked();
  expect(ui.saveScim.get()).toBeDisabled();

  await user.click(ui.scimProvisioningButton.get());
  expect(ui.saveScim.get()).toBeEnabled();
  await user.click(ui.saveScim.get());
  await user.click(ui.confirmProvisioningButton.get());

  expect(await ui.scimProvisioningButton.find()).toBeChecked();
  expect(await ui.saveScim.find()).toBeDisabled();
});

it('should not allow editions below Enterprise to select SCIM provisioning', async () => {
  const user = userEvent.setup();

  renderAuthentication();

  await ui.createConfiguration(user);
  await user.click(await ui.enableConfigButton.find());

  expect(await ui.jitProvisioningButton.find()).toBeChecked();
  expect(ui.scimProvisioningButton.get()).toHaveAttribute('aria-disabled', 'true');
});

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>,
  );
}
