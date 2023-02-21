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
import {
  mockAlmSettingsInstance,
  mockProjectAlmBindingResponse,
} from '../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderApp, RenderContext } from '../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import {
  getCommonNodes,
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import { TutorialModes } from '../../types';
import GitHubActionTutorial, { GitHubActionTutorialProps } from '../GitHubActionTutorial';

jest.mock('../../../../api/user-tokens');

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

const tokenMock = new UserTokensMock();

afterEach(() => {
  tokenMock.reset();
});

const ui = {
  ...getCommonNodes(TutorialModes.GitHubActions),
  ...getTutorialActionButtons(),
  ...getTutorialBuildButtons(),
};

it('should follow and complete all steps', async () => {
  const user = userEvent.setup();
  renderGithubActionTutorial();

  expect(await ui.secretsStepTitle.find()).toBeInTheDocument();

  // Env variables step
  expect(getCopyToClipboardValue()).toMatchSnapshot('sonar token key');
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('sonarqube host url key');
  expect(getCopyToClipboardValue(2)).toMatchSnapshot('sonarqube host url value');
  await user.click(ui.continueButton.get());

  // Create/update configuration file step
  // Maven
  await user.click(ui.mavenBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('Maven: .github/workflows/build.yml');

  // Gradle
  await user.click(ui.gradleBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('Gradle: build.gradle');
  expect(getCopyToClipboardValue(3)).toMatchSnapshot('Gradle: .github/workflows/build.yml');

  // .NET
  await user.click(ui.dotnetBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('.NET: .github/workflows/build.yml');

  // CFamily
  await user.click(ui.cFamilyBuildButton.get());
  expect(getCopyToClipboardValue()).toMatchSnapshot('CFamily: sonar-project.properties');
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue(2)).toMatchSnapshot('CFamily Linux: .github/workflows/build.yml');
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue(2)).toMatchSnapshot(
    'CFamily Windows: .github/workflows/build.yml'
  );
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue(2)).toMatchSnapshot('CFamily MacOS: .github/workflows/build.yml');

  // Other
  await user.click(ui.otherBuildButton.get());
  expect(getCopyToClipboardValue()).toMatchSnapshot('Other: sonar-project.properties');
  expect(getCopyToClipboardValue(2)).toMatchSnapshot('Other: .github/workflows/build.yml');

  await user.click(ui.finishTutorialButton.get());
  expect(ui.allSetSentence.get()).toBeInTheDocument();
});

it('should generate/delete a new token or use existing one', async () => {
  const user = userEvent.setup();
  renderGithubActionTutorial();

  expect(await ui.secretsStepTitle.find()).toBeInTheDocument();

  // Generate token
  await user.click(ui.genTokenDialogButton.get());
  await user.click(ui.generateTokenButton.get());
  expect(getCopyToClipboardValue(3)).toEqual('generatedtoken2');

  // Revoke current token and create new one
  await user.click(ui.deleteTokenButton.get());
  await user.type(ui.tokenNameInput.get(), 'newtoken');
  await selectEvent.select(ui.expiresInSelect.get(), 'users.tokens.expiration.365');
  await user.click(ui.generateTokenButton.get());
  expect(ui.tokenValue.get()).toBeInTheDocument();
  await user.click(ui.continueButton.getAll()[1]);
  expect(ui.tokenValue.query()).not.toBeInTheDocument();
});

it('navigates between steps', async () => {
  const user = userEvent.setup();
  renderGithubActionTutorial({
    almBinding: mockAlmSettingsInstance({
      alm: AlmKeys.GitHub,
      url: 'http://localhost/qube',
    }),
    projectBinding: mockProjectAlmBindingResponse({ alm: AlmKeys.GitHub }),
  });

  // If project is bound, link to repo is visible
  expect(await ui.linkToRepo.find()).toBeInTheDocument();

  await user.click(await ui.continueButton.find());
  await user.click(ui.mavenBuildButton.get());
  await user.click(ui.finishTutorialButton.get());
  expect(ui.allSetSentence.get()).toBeInTheDocument();

  await user.click(ui.ymlFileStepTitle.get());
  expect(ui.mavenBuildButton.get()).toBeInTheDocument();
  await user.click(ui.secretsStepTitle.get());
  expect(ui.genTokenDialogButton.get()).toBeInTheDocument();
});

function renderGithubActionTutorial(
  overrides: Partial<GitHubActionTutorialProps> = {},
  { languages = { c: mockLanguage({ key: 'c' }) } }: RenderContext = {}
) {
  return renderApp(
    '/',
    <GitHubActionTutorial
      baseUrl="http://localhost:9000"
      mainBranchName="main"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      {...overrides}
    />,
    { languages, featureList: [Feature.BranchSupport] }
  );
}
