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
import { renderApp, RenderContext } from '../../../../helpers/testReactTestingUtils';
import {
  getCommonNodes,
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import { TutorialModes } from '../../types';
import GitLabCITutorial, { GitLabCITutorialProps } from '../GitLabCITutorial';

jest.mock('../../../../api/user-tokens');

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

const tokenMock = new UserTokensMock();

afterEach(() => {
  tokenMock.reset();
});

const ui = {
  ...getCommonNodes(TutorialModes.GitLabCI),
  ...getTutorialActionButtons(),
  ...getTutorialBuildButtons(),
};

it('should follow and complete all steps', async () => {
  const user = userEvent.setup();
  renderGitLabTutorial();

  expect(await ui.secretsStepTitle.find()).toBeInTheDocument();

  // Env variables step
  expect(getCopyToClipboardValue()).toMatchSnapshot('sonar token key');
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('sonarqube host url key');
  expect(getCopyToClipboardValue(2)).toMatchSnapshot('sonarqube host url value');
  await user.click(ui.continueButton.get());

  // Create/update configuration file step
  // Maven
  await user.click(ui.mavenBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('Maven: pom.xml');
  expect(getCopyToClipboardValue(3)).toMatchSnapshot('Maven: gitlab-ci.yml');

  // Gradle
  await user.click(ui.gradleBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('Gradle: build.gradle');
  expect(getCopyToClipboardValue(3)).toMatchSnapshot('Gradle: gitlab-ci.yml');

  // .NET
  await user.click(ui.dotnetBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('.NET: gitlab-ci.yml');

  // CFamily
  await user.click(ui.cFamilyBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('CFamily: sonar-project.properties');
  expect(getCopyToClipboardValue(3)).toMatchSnapshot('CFamily: gitlab-ci.yml');

  // Other
  await user.click(ui.otherBuildButton.get());
  expect(getCopyToClipboardValue(1)).toMatchSnapshot('Other: sonar-project.properties');
  expect(getCopyToClipboardValue(3)).toMatchSnapshot('Other: gitlab-ci.yml');

  await user.click(ui.finishTutorialButton.get());
  expect(ui.allSetSentence.get()).toBeInTheDocument();
});

it('should generate/delete a new token or use existing one', async () => {
  const user = userEvent.setup();
  renderGitLabTutorial();

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
  renderGitLabTutorial();

  await user.click(await ui.continueButton.find());
  await user.click(ui.mavenBuildButton.get());
  await user.click(ui.finishTutorialButton.get());
  expect(ui.allSetSentence.get()).toBeInTheDocument();

  await user.click(ui.ymlFileStepTitle.get());
  expect(ui.finishTutorialButton.get()).toBeInTheDocument();
  await user.click(ui.secretsStepTitle.get());
  expect(ui.continueButton.get()).toBeInTheDocument();
});

function renderGitLabTutorial(
  overrides: Partial<GitLabCITutorialProps> = {},
  { languages = { c: mockLanguage({ key: 'c' }) } }: RenderContext = {}
) {
  return renderApp(
    '/',
    <GitLabCITutorial
      baseUrl="http://localhost:9000"
      mainBranchName="main"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      {...overrides}
    />,
    { languages }
  );
}
