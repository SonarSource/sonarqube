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
import userEvent from '@testing-library/user-event';
import React from 'react';
import { byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import { Feature } from '../../../../../types/features';
import Authentication from '../Authentication';

let settingsHandler: SettingsServiceMock;

beforeEach(() => {
  settingsHandler = new SettingsServiceMock();
  settingsHandler.setDefinitions(definitions);
});

afterEach(() => {
  settingsHandler.reset();
});

const enabledDefinition = byTestId('sonar.auth.bitbucket.enabled');
const consumerKeyDefinition = byTestId('sonar.auth.bitbucket.clientId.secured');
const consumerSecretDefinition = byTestId('sonar.auth.bitbucket.clientSecret.secured');
const allowUsersToSignUpDefinition = byTestId('sonar.auth.bitbucket.allowUsersToSignUp');
const workspacesDefinition = byTestId('sonar.auth.bitbucket.workspaces');

const ui = {
  save: byRole('button', { name: 'save' }),
  cancel: byRole('button', { name: 'cancel' }),
  reset: byRole('button', { name: /settings.definition.reset/ }),
  confirmReset: byRole('dialog').byRole('button', { name: 'reset_verb' }),
  change: byRole('button', { name: 'change_verb' }),
  enabledDefinition,
  enabled: enabledDefinition.byRole('switch'),
  consumerKeyDefinition,
  consumerKey: consumerKeyDefinition.byRole('textbox'),
  consumerSecretDefinition,
  consumerSecret: consumerSecretDefinition.byRole('textbox'),
  allowUsersToSignUpDefinition,
  allowUsersToSignUp: allowUsersToSignUpDefinition.byRole('switch'),
  workspacesDefinition,
  workspaces: workspacesDefinition.byRole('textbox'),
  workspacesDelete: workspacesDefinition.byRole('button', {
    name: /settings.definition.delete_value/,
  }),
  insecureWarning: byText(/settings.authentication.gitlab.configuration.insecure/),
};

it('should show warning if sign up is enabled and there are no workspaces', async () => {
  renderAuthentication();
  const user = userEvent.setup();

  expect(await ui.allowUsersToSignUpDefinition.find()).toBeInTheDocument();
  expect(ui.allowUsersToSignUp.get()).toBeChecked();
  expect(ui.workspaces.get()).toHaveValue('');
  expect(ui.insecureWarning.get()).toBeInTheDocument();

  await user.click(ui.allowUsersToSignUp.get());
  await user.click(ui.allowUsersToSignUpDefinition.by(ui.save).get());
  expect(ui.allowUsersToSignUp.get()).not.toBeChecked();
  expect(ui.insecureWarning.query()).not.toBeInTheDocument();

  await user.click(ui.allowUsersToSignUp.get());
  await user.click(ui.allowUsersToSignUpDefinition.by(ui.save).get());
  expect(ui.allowUsersToSignUp.get()).toBeChecked();
  expect(await ui.insecureWarning.find()).toBeInTheDocument();

  await user.type(ui.workspaces.get(), 'test');
  await user.click(ui.workspacesDefinition.by(ui.save).get());
  expect(ui.insecureWarning.query()).not.toBeInTheDocument();

  await user.click(ui.workspacesDefinition.by(ui.reset).get());
  await user.click(ui.confirmReset.get());
  expect(await ui.insecureWarning.find()).toBeInTheDocument();
});

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>,
    `?tab=${AlmKeys.BitbucketServer}`,
  );
}
