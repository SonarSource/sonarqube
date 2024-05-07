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
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import AlmSettingsServiceMock from '../../../../../api/mocks/AlmSettingsServiceMock';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import AlmBindingDefinitionForm, {
  AlmBindingDefinitionFormProps,
} from '../AlmBindingDefinitionForm';

jest.mock('../../../../../api/alm-settings');

let almSettings: AlmSettingsServiceMock;

beforeAll(() => {
  almSettings = new AlmSettingsServiceMock();
});

afterEach(() => {
  almSettings.reset();
});

const ui = {
  bitbucketConfiguration: (almKey: AlmKeys.BitbucketCloud | AlmKeys.BitbucketServer) =>
    byRole('button', { name: `alm.${almKey}.long` }),
  configurationInput: (id: string) =>
    byLabelText(`settings.almintegration.form.${id}`, { exact: false }),
  saveConfigurationButton: byRole('button', { name: 'settings.almintegration.form.save' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  validationError: (text: string) => byText(text),
};

const onCancel = jest.fn();

it('enforceValidation enabled', async () => {
  almSettings.setDefinitionErrorMessage('Validation Error');
  renderAlmBindingDefinitionForm();

  // Fill in form
  await userEvent.type(await ui.configurationInput('name.gitlab').find(), 'Name');
  await userEvent.type(ui.configurationInput('url.gitlab').get(), 'https://api.alm.com');
  await userEvent.type(ui.configurationInput('personal_access_token').get(), 'Access Token');

  await userEvent.click(ui.saveConfigurationButton.get());
  expect(ui.validationError('Validation Error').get()).toBeInTheDocument();

  await userEvent.click(ui.cancelButton.get());
  expect(onCancel).toHaveBeenCalled();
});

function renderAlmBindingDefinitionForm(props: Partial<AlmBindingDefinitionFormProps> = {}) {
  return renderComponent(
    <AlmBindingDefinitionForm
      onCancel={onCancel}
      afterSubmit={jest.fn()}
      enforceValidation
      alm={AlmKeys.GitLab}
      {...props}
    />,
  );
}
