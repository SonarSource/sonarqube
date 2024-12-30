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
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import AlmSettingsServiceMock from '../../../../../api/mocks/AlmSettingsServiceMock';
import CurrentUserContextProvider from '../../../../../app/components/current-user/CurrentUserContextProvider';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockCurrentUser } from '../../../../../helpers/testMocks';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import {
  AlmKeys,
  ProjectAlmBindingConfigurationErrorScope,
} from '../../../../../types/alm-settings';
import { Component } from '../../../../../types/types';
import { CurrentUser } from '../../../../../types/users';
import PRDecorationBinding from '../PRDecorationBinding';

let almSettings: AlmSettingsServiceMock;

beforeAll(() => {
  almSettings = new AlmSettingsServiceMock();
});

afterEach(() => {
  almSettings.reset();
});

const inputsList = {
  [AlmKeys.GitLab]: { 'gitlab.repository': 'Repository', monorepo: false },
  [AlmKeys.GitHub]: {
    'github.repository': 'Repository',
    'github.summary_comment_setting': false,
    monorepo: false,
  },
  [AlmKeys.Azure]: {
    'azure.repository': 'Repository',
    'azure.project': 'Project',
    monorepo: false,
  },
  [AlmKeys.BitbucketCloud]: { 'bitbucketcloud.repository': 'Repository', monorepo: false },
  [AlmKeys.BitbucketServer]: {
    'bitbucket.repository': 'Repository',
    'bitbucket.slug': 'Slug',
    monorepo: false,
  },
};

it.each([
  {
    key: 'conf-final-1',
    alm: AlmKeys.GitLab,
  },
  {
    key: 'conf-github-1',
    alm: AlmKeys.GitHub,
  },
  {
    key: 'conf-azure-1',
    alm: AlmKeys.Azure,
  },
  {
    key: 'conf-bitbucketcloud-1',
    alm: AlmKeys.BitbucketCloud,
  },
  {
    key: 'conf-bitbucketserver-1',
    alm: AlmKeys.BitbucketServer,
  },
])(
  'should get, set, delete and validate binding for $alm',
  async ({ key, alm }: { alm: AlmKeys; key: string }) => {
    const { ui, user } = getPageObjects();
    almSettings.setProjectBindingConfigurationErrors({
      scope: ProjectAlmBindingConfigurationErrorScope.Global,
      errors: [{ msg: 'cute error' }],
    });
    const { rerender } = renderPRDecorationBinding();
    expect(await ui.mainTitle.find()).toBeInTheDocument();

    await user.click(ui.input('name', 'combobox').get());
    await user.click(byRole('option', { name: new RegExp(key) }).get());

    const list = inputsList[alm];
    for (const [inputId, value] of Object.entries(list)) {
      // eslint-disable-next-line no-await-in-loop
      await ui.setInput(inputId, value);
    }
    // Save form and check for errors
    await user.click(ui.saveButton.get());
    expect(await ui.validationMsg('cute error').find()).toBeInTheDocument();

    // Check validation with errors
    await user.click(ui.validateButton.get());
    expect(ui.validationMsg('cute error').get()).toBeInTheDocument();

    // Save form and check for errors
    almSettings.setProjectBindingConfigurationErrors(undefined);
    await ui.setInput(
      Object.keys(list).find((key) => key.endsWith('.repository')) as string,
      'Anything',
    );
    await user.click(ui.saveButton.get());
    expect(
      await ui.validationMsg('settings.pr_decoration.binding.check_configuration.success').find(),
    ).toBeInTheDocument();

    await user.click(ui.validateButton.get());
    expect(
      ui.validationMsg('settings.pr_decoration.binding.check_configuration.success').get(),
    ).toBeInTheDocument();

    // Rerender and verify that validation is done for binding
    rerender(
      <MockedPRDecorationBinding component={mockComponent()} currentUser={mockCurrentUser()} />,
    );
    expect(
      await ui.validationMsg('settings.pr_decoration.binding.check_configuration.success').find(),
    ).toBeInTheDocument();
    expect(ui.saveButton.query()).not.toBeInTheDocument();

    // Reset binding
    await user.click(ui.resetButton.get());
    expect(ui.input('', 'textbox').query()).not.toBeInTheDocument();
    expect(ui.input('', 'switch').query()).not.toBeInTheDocument();
  },
);

function getPageObjects() {
  const user = userEvent.setup();

  async function setInput(inputId: string, value: string | boolean) {
    if (typeof value === 'boolean') {
      if (value) {
        await user.click(ui.input(inputId, 'switch').get());
      }
    } else {
      const input = ui.input(inputId, 'textbox').get();
      await user.clear(input);
      await user.type(input, value);
    }
  }

  const ui = {
    mainTitle: byRole('heading', { name: 'settings.pr_decoration.binding.title' }),
    input: (id: string, role: 'combobox' | 'switch' | 'textbox') =>
      byRole(role, { name: new RegExp(`settings.pr_decoration.binding.form.${id}`) }),
    saveButton: byRole('button', { name: 'save' }),
    resetButton: byRole('button', { name: 'reset_verb' }),
    validateButton: byRole('button', {
      name: 'settings.pr_decoration.binding.check_configuration',
    }),
    validationMsg: (text: string) => byText(text),
    setInput,
  };

  return {
    ui,
    user,
  };
}

function MockedPRDecorationBinding({
  component,
  currentUser,
}: {
  component: Component;
  currentUser: CurrentUser;
}) {
  return (
    <CurrentUserContextProvider currentUser={currentUser}>
      <PRDecorationBinding component={component} />
    </CurrentUserContextProvider>
  );
}

function renderPRDecorationBinding(
  component: Component = mockComponent(),
  currentUser: CurrentUser = mockCurrentUser(),
) {
  return renderComponent(
    <MockedPRDecorationBinding component={component} currentUser={currentUser} />,
  );
}
