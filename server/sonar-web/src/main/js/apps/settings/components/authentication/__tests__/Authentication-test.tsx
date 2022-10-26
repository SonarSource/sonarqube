/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import React from 'react';
import AuthenticationServiceMock from '../../../../../api/mocks/AuthenticationServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { mockDefinition } from '../../../../../helpers/mocks/settings';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../../types/features';
import { ExtendedSettingDefinition, SettingType } from '../../../../../types/settings';
import Authentication from '../Authentication';

jest.mock('../../../../../api/settings');

const mockDefinitionFields = [
  mockDefinition({
    key: 'test1',
    category: 'authentication',
    subCategory: 'saml',
    name: 'test1',
    description: 'desc1'
  }),
  mockDefinition({
    key: 'test2',
    category: 'authentication',
    subCategory: 'saml',
    name: 'test2',
    description: 'desc2'
  }),
  mockDefinition({
    key: 'sonar.auth.saml.certificate.secured',
    category: 'authentication',
    subCategory: 'saml',
    name: 'Certificate',
    description: 'Secured certificate',
    type: SettingType.PASSWORD
  }),
  mockDefinition({
    key: 'sonar.auth.saml.enabled',
    category: 'authentication',
    subCategory: 'saml',
    name: 'Enabled',
    description: 'To enable the flag',
    type: SettingType.BOOLEAN
  })
];

let handler: AuthenticationServiceMock;

beforeEach(() => {
  handler = new AuthenticationServiceMock();
});

afterEach(() => handler.resetValues());

it('should render tabs and allow navigation', async () => {
  const user = userEvent.setup();
  renderAuthentication([]);

  expect(screen.getAllByRole('tab')).toHaveLength(4);

  expect(screen.getByRole('tab', { name: 'SAML' })).toHaveAttribute('aria-selected', 'true');

  await user.click(screen.getByRole('tab', { name: 'github GitHub' }));

  expect(screen.getByRole('tab', { name: 'SAML' })).toHaveAttribute('aria-selected', 'false');
  expect(screen.getByRole('tab', { name: 'github GitHub' })).toHaveAttribute(
    'aria-selected',
    'true'
  );
});

describe('SAML tab', () => {
  it('should allow user to test the configuration', async () => {
    const user = userEvent.setup();

    const definitions = [
      mockDefinition({
        key: 'sonar.auth.saml.certificate.secured',
        category: 'authentication',
        subCategory: 'saml',
        name: 'Certificate',
        description: 'Secured certificate',
        type: SettingType.PASSWORD
      }),
      mockDefinition({
        key: 'sonar.auth.saml.enabled',
        category: 'authentication',
        subCategory: 'saml',
        name: 'Enabled',
        description: 'To enable the flag',
        type: SettingType.BOOLEAN
      })
    ];

    renderAuthentication(definitions);

    await user.click(await screen.findByText('settings.almintegration.form.secret.update_field'));

    await user.click(screen.getByRole('textbox', { name: 'Certificate' }));
    await user.keyboard('new certificate');

    expect(screen.getByText('settings.authentication.saml.form.test')).toHaveClass('disabled');

    await user.click(
      screen.getByRole('button', { name: 'settings.authentication.saml.form.save' })
    );

    expect(screen.getByText('settings.authentication.saml.form.test')).not.toHaveClass('disabled');
  });

  it('should allow user to edit fields and save configuration', async () => {
    const user = userEvent.setup();
    const definitions = mockDefinitionFields;
    renderAuthentication(definitions);

    expect(screen.getByRole('button', { name: 'off' })).toHaveAttribute('aria-disabled', 'true');
    // update fields
    await user.click(screen.getByRole('textbox', { name: 'test1' }));
    await user.keyboard('new test1');

    await user.click(screen.getByRole('textbox', { name: 'test2' }));
    await user.keyboard('new test2');
    // check if enable is allowed after updating
    expect(screen.getByRole('button', { name: 'off' })).toHaveAttribute('aria-disabled', 'false');

    // reset value
    await user.click(screen.getByRole('textbox', { name: 'test2' }));
    await user.keyboard('{Control>}a{/Control}{Backspace}');
    await user.click(
      screen.getByRole('button', { name: 'settings.authentication.saml.form.save' })
    );
    expect(screen.getByRole('button', { name: 'off' })).toHaveAttribute('aria-disabled', 'true');

    await user.click(screen.getByRole('textbox', { name: 'test2' }));
    await user.keyboard('new test2');
    expect(screen.getByRole('button', { name: 'off' })).toHaveAttribute('aria-disabled', 'false');

    expect(
      screen.getByRole('button', { name: 'settings.almintegration.form.secret.update_field' })
    ).toBeInTheDocument();
    await user.click(
      screen.getByRole('button', { name: 'settings.almintegration.form.secret.update_field' })
    );
    // check for secure fields
    expect(screen.getByRole('textbox', { name: 'Certificate' })).toBeInTheDocument();
    await user.click(screen.getByRole('textbox', { name: 'Certificate' }));
    await user.keyboard('new certificate');
    // enable the configuration
    await user.click(screen.getByRole('button', { name: 'off' }));
    expect(screen.getByRole('button', { name: 'on' })).toBeInTheDocument();

    await user.click(
      screen.getByRole('button', { name: 'settings.authentication.saml.form.save' })
    );
    expect(screen.getByText('settings.authentication.saml.form.save_success')).toBeInTheDocument();
    // check after switching tab that the flag is still enabled
    await user.click(screen.getByRole('tab', { name: 'github GitHub' }));
    await user.click(screen.getByRole('tab', { name: 'SAML' }));

    expect(screen.getByRole('button', { name: 'on' })).toBeInTheDocument();
  });

  it('should handle and show errors to the user', async () => {
    const user = userEvent.setup();
    const definitions = mockDefinitionFields;
    renderAuthentication(definitions);

    await user.click(screen.getByRole('textbox', { name: 'test1' }));
    await user.keyboard('value');
    await user.click(screen.getByRole('textbox', { name: 'test2' }));
    await user.keyboard('{Control>}a{/Control}error');
    await user.click(
      screen.getByRole('button', { name: 'settings.authentication.saml.form.save' })
    );
    expect(screen.getByText('settings.authentication.saml.form.save_partial')).toBeInTheDocument();
  });

  it('should not display the login message feature info box', () => {
    renderAuthentication([]);

    expect(
      screen.queryByText('settings.authentication.custom_message_information')
    ).not.toBeInTheDocument();
  });

  it('should display the login message feature info box', () => {
    renderAuthentication([], [Feature.LoginMessage]);

    expect(
      screen.getByText('settings.authentication.custom_message_information')
    ).toBeInTheDocument();
  });
});

function renderAuthentication(definitions: ExtendedSettingDefinition[], features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>
  );
}
