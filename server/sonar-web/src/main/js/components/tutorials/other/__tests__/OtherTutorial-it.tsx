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
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { RenderContext, renderApp } from '../../../../helpers/testReactTestingUtils';
import {
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import OtherTutorial from '../OtherTutorial';

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

jest.mock('clipboard', () => ({
  copy: jest.fn(),
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
  await user.click(ui.expiresInSelect.get());
  await user.click(byRole('option', { name: 'users.tokens.expiration.365' }).get());

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
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot('maven: execute scanner');

  // Gradle
  await user.click(ui.gradleBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'gradle: sonarqube plugin',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'gradle: execute scanner',
  );

  // Dotnet - Core
  await user.click(ui.dotnetBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'dotnet core: install scanner globally',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'dotnet core: execute command 1',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'dotnet core: execute command 2',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'dotnet core: execute command 3',
  );

  // Dotnet - Framework
  await user.click(ui.dotnetFrameworkButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'dotnet framework: execute command 1',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'dotnet framework: execute command 2',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'dotnet framework: execute command 3',
  );

  // C++ - Automatic
  await user.click(ui.cppBuildButton.get());
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other linux: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other linux: execute scanner',
  );
  await user.click(ui.arm64Button.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other linux arm64: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other linux arm64: execute scanner',
  );
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other windows: execute scanner',
  );
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other macos: execute scanner',
  );

  // C++ - Linux (x86_64)
  await user.click(ui.autoConfigManual.get());
  await user.click(ui.linuxButton.get());
  await user.click(ui.x86_64Button.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux: execute scanner',
  );

  // C++ - Linux (ARM64)
  await user.click(ui.arm64Button.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux arm64: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux arm64: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux arm64: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) linux arm64: execute scanner',
  );

  // C++ - Windows
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) windows: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) windows: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) windows: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) windows: execute scanner',
  );

  // C++ - MacOS
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) macos: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) macos: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) macos: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'c++ (manual) macos: execute scanner',
  );

  // Objective-C - Linux (x86_64)
  await user.click(ui.objCBuildButton.get());
  await user.click(ui.linuxButton.get());
  await user.click(ui.x86_64Button.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux: execute scanner',
  );

  // Objective-C - Linux (ARM64)
  await user.click(ui.arm64Button.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux arm64: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux arm64: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux arm64: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'objective-c linux arm64: execute scanner',
  );

  // Objective-C - Windows
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'objective-c windows: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'objective-c windows: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'objective-c windows: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'objective-c windows: execute scanner',
  );

  // Objective-C - MacOS
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'objective-c macos: download build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'objective-c macos: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 2, name: 'Copy' })).toMatchSnapshot(
    'objective-c macos: execute build wrapper',
  );
  expect(getCopyToClipboardValue({ i: 3, name: 'Copy' })).toMatchSnapshot(
    'objective-c macos: execute scanner',
  );

  // Dart - Linux
  await user.click(ui.dartBuildButton.get());
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'Dart linux: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'Dart linux: execute scanner',
  );

  // Dart - Windows
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'Dart windows: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'Dart windows: execute scanner',
  );

  // Dart - MacOS
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'Dart macos: download scanner',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'Dart macos: execute scanner',
  );

  // Other - Linux
  await user.click(ui.otherBuildButton.get());
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other linux: execute scanner',
  );

  // Other - Windows
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other windows: execute scanner',
  );

  // Other - MacOS
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'c++ (automatic) and other macos: execute scanner',
  );
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
