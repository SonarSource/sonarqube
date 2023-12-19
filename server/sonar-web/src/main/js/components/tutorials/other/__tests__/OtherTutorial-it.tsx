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

import userEvent from '@testing-library/user-event';
import React from 'react';
import selectEvent from 'react-select-event';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { RenderContext, renderApp } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import {
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import OtherTutorial from '../OtherTutorial';

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

const tokenMock = new UserTokensMock();

afterEach(() => {
  tokenMock.reset();
});

const ui = {
  provideTokenTitle: byRole('heading', { name: 'onboarding.token.header' }),
  runAnalysisTitle: byRole('heading', { name: 'onboarding.analysis.header' }),
  generateTokenRadio: byRole('radio', { name: 'onboarding.token.generate.PROJECT_ANALYSIS_TOKEN' }),
  existingTokenRadio: byRole('radio', { name: 'onboarding.token.use_existing_token' }),
  tokenNameInput: byRole('textbox', { name: /onboarding.token.name.label/ }),
  expiresInSelect: byRole('combobox', { name: '' }),
  tokenValueInput: byRole('textbox', { name: /onboarding.token.use_existing_token.label/ }),
  invalidTokenValueMessage: byText('onboarding.token.invalid_format'),
  ...getTutorialActionButtons(),
  ...getTutorialBuildButtons(),
};

it('should generate/delete a new token or use existing one', async () => {
  const user = userEvent.setup();
  renderOtherTutorial();

  // Verify that pages is rendered and includes 2 steps
  expect(await ui.provideTokenTitle.find()).toBeInTheDocument();
  expect(ui.runAnalysisTitle.get()).toBeInTheDocument();

  // Generating token
  await user.type(ui.tokenNameInput.get(), 'Testing token');
  await selectEvent.select(ui.expiresInSelect.get(), 'users.tokens.expiration.365');
  await user.click(ui.generateTokenButton.get());

  expect(ui.continueButton.get()).toBeEnabled();

  // Deleting generated token & switchning to existing one
  await user.click(ui.deleteTokenButton.get());

  await user.click(ui.existingTokenRadio.get());
  await user.type(ui.tokenValueInput.get(), 'INVALID TOKEN VALUE');
  expect(ui.invalidTokenValueMessage.get()).toBeInTheDocument();

  await user.clear(ui.tokenValueInput.get());
  await user.type(ui.tokenValueInput.get(), 'validtokenvalue');
  expect(ui.continueButton.get()).toBeEnabled();

  // navigate to 'Run analysis' step
  await user.click(ui.continueButton.get());
  expect(ui.describeBuildTitle.get()).toBeInTheDocument();

  // navigate to previous step
  await user.click(ui.provideTokenTitle.get());
  expect(ui.continueButton.get()).toBeEnabled();
});

it('can choose build tools and copy provided settings', async () => {
  const user = userEvent.setup();
  renderOtherTutorial();

  await user.click(ui.generateTokenButton.get());
  await user.click(ui.continueButton.get());

  // Maven
  await user.click(ui.mavenBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('maven: execute scanner');

  // Gradle
  await user.click(ui.gradleBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('gradle: sonarqube plugin');
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('gradle: execute scanner');

  // Dotnet - Core
  await user.click(ui.dotnetBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'dotnet core: install scanner globally',
  );
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('dotnet core: execute command 1');
  expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot('dotnet core: execute command 2');
  expect(getCopyToClipboardValue(3, 'Copy')).toMatchSnapshot('dotnet core: execute command 3');

  // Dotnet - Framework
  await user.click(ui.dotnetFrameworkButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('dotnet framework: execute command 1');
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('dotnet framework: execute command 2');
  expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot('dotnet framework: execute command 3');

  // C Family - Linux
  await user.click(ui.cFamilyBuildButton.get());
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'cfamily linux: execute build wrapper',
  );
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('cfamily linux: execute scanner');

  // C Family - Windows
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'cfamily windows: execute build wrapper',
  );
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('cfamily windows: execute scanner');

  // C Family - MacOS
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'cfamily macos: execute build wrapper',
  );
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('cfamily macos: execute scanner');

  // Other - Linux
  await user.click(ui.otherBuildButton.get());
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('other linux: execute scanner');

  // Other - Windows
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('other windows: execute scanner');

  // Other - MacOS
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('other macos: execute scanner');
});

function renderOtherTutorial({
  languages = { c: mockLanguage({ key: 'c' }) },
}: RenderContext = {}) {
  return renderApp(
    '/',
    <OtherTutorial
      baseUrl="http://localhost:9000"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
    />,
    { languages },
  );
}
