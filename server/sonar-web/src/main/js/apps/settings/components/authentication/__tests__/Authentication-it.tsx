/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import React from 'react';
import { byRole, byText } from 'testing-library-selector';
import AuthenticationServiceMock from '../../../../../api/mocks/AuthenticationServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../../types/features';
import Authentication from '../Authentication';

jest.mock('../../../../../api/settings');

let handler: AuthenticationServiceMock;

beforeEach(() => {
  handler = new AuthenticationServiceMock();
});

afterEach(() => handler.resetValues());

const ui = {
  saveButton: byRole('button', { name: 'settings.authentication.saml.form.save' }),
  customMessageInformation: byText('settings.authentication.custom_message_information'),
  enabledToggle: byRole('switch'),
  testButton: byText('settings.authentication.saml.form.test'),
  textbox1: byRole('textbox', { name: 'test1' }),
  textbox2: byRole('textbox', { name: 'test2' }),
  saml: {
    noSamlConfiguration: byText('settings.authentication.saml.form.not_configured'),
    createConfigButton: byRole('button', { name: 'settings.authentication.form.create' }),
    providerName: byRole('textbox', { name: 'Provider Name' }),
    providerId: byRole('textbox', { name: 'Provider ID' }),
    providerCertificate: byRole('textbox', { name: 'Identity provider certificate' }),
    loginUrl: byRole('textbox', { name: 'SAML login url' }),
    userLoginAttribute: byRole('textbox', { name: 'SAML user login attribute' }),
    userNameAttribute: byRole('textbox', { name: 'SAML user name attribute' }),
    saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
    confirmProvisioningButton: byRole('button', { name: 'yes' }),
    saveScim: byRole('button', { name: 'save' }),
    groupAttribute: byRole('textbox', { name: 'property.sonar.auth.saml.group.name.name' }),
    enableConfigButton: byRole('button', { name: 'settings.authentication.saml.form.enable' }),
    disableConfigButton: byRole('button', { name: 'settings.authentication.saml.form.disable' }),
    editConfigButton: byRole('button', { name: 'settings.authentication.form.edit' }),
    enableFirstMessage: byText('settings.authentication.saml.enable_first'),
    jitProvisioningButton: byRole('radio', {
      name: 'settings.authentication.saml.form.provisioning_at_login',
    }),
    scimProvisioningButton: byRole('radio', {
      name: 'settings.authentication.saml.form.provisioning_with_scim',
    }),
    fillForm: async (user: UserEvent) => {
      const { saml } = ui;
      await act(async () => {
        await user.clear(saml.providerName.get());
        await user.type(saml.providerName.get(), 'Awsome SAML config');
        await user.type(saml.providerId.get(), 'okta-1234');
        await user.type(saml.loginUrl.get(), 'http://test.org');
        await user.type(saml.providerCertificate.get(), '-secret-');
        await user.type(saml.userLoginAttribute.get(), 'login');
        await user.type(saml.userNameAttribute.get(), 'name');
      });
    },
    createConfiguration: async (user: UserEvent) => {
      const { saml } = ui;
      await act(async () => {
        await user.click(await saml.createConfigButton.find());
      });
      await saml.fillForm(user);
      await act(async () => {
        await user.click(saml.saveConfigButton.get());
      });
    },
  },
};

it('should render tabs and allow navigation', async () => {
  const user = userEvent.setup();
  renderAuthentication();

  expect(screen.getAllByRole('tab')).toHaveLength(4);

  expect(screen.getByRole('tab', { name: 'SAML' })).toHaveAttribute('aria-selected', 'true');

  await user.click(screen.getByRole('tab', { name: 'github GitHub' }));

  expect(screen.getByRole('tab', { name: 'SAML' })).toHaveAttribute('aria-selected', 'false');
  expect(screen.getByRole('tab', { name: 'github GitHub' })).toHaveAttribute(
    'aria-selected',
    'true'
  );
});

it('should not display the login message feature info box', () => {
  renderAuthentication();

  expect(ui.customMessageInformation.query()).not.toBeInTheDocument();
});

it('should display the login message feature info box', () => {
  renderAuthentication([Feature.LoginMessage]);

  expect(ui.customMessageInformation.get()).toBeInTheDocument();
});

describe('SAML tab', () => {
  const { saml } = ui;

  it('should render an empty SAML configuration', async () => {
    renderAuthentication();
    expect(await saml.noSamlConfiguration.find()).toBeInTheDocument();
  });

  it('should be able to create a configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();

    await user.click(await saml.createConfigButton.find());

    expect(saml.saveConfigButton.get()).toBeDisabled();
    await saml.fillForm(user);
    expect(saml.saveConfigButton.get()).toBeEnabled();

    await act(async () => {
      await user.click(saml.saveConfigButton.get());
    });

    expect(await saml.editConfigButton.find()).toBeInTheDocument();
  });

  it('should be able to enable/disable configuration', async () => {
    const { saml } = ui;
    const user = userEvent.setup();
    renderAuthentication();

    await saml.createConfiguration(user);
    await user.click(await saml.enableConfigButton.find());

    expect(await saml.disableConfigButton.find()).toBeInTheDocument();
    await user.click(saml.disableConfigButton.get());
    expect(saml.disableConfigButton.query()).not.toBeInTheDocument();

    expect(await saml.enableConfigButton.find()).toBeInTheDocument();
  });

  it('should be able to choose provisioning', async () => {
    const { saml } = ui;
    const user = userEvent.setup();

    renderAuthentication([Feature.Scim]);

    await saml.createConfiguration(user);

    expect(await saml.enableFirstMessage.find()).toBeInTheDocument();
    await user.click(await saml.enableConfigButton.find());

    expect(await saml.jitProvisioningButton.find()).toBeChecked();

    await user.type(saml.groupAttribute.get(), 'group');
    expect(saml.saveScim.get()).toBeEnabled();
    await user.click(saml.saveScim.get());
    expect(await saml.saveScim.find()).toBeDisabled();

    await user.click(saml.scimProvisioningButton.get());
    expect(saml.saveScim.get()).toBeEnabled();
    await user.click(saml.saveScim.get());
    await user.click(saml.confirmProvisioningButton.get());

    expect(await saml.scimProvisioningButton.find()).toBeChecked();
    expect(await saml.saveScim.find()).toBeDisabled();
  });
});

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>
  );
}
