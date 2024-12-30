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
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { RenderContext, renderApp } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../sonar-aligned/helpers/testSelector';
import {
  getCommonNodes,
  getCopyToClipboardHostURLValue,
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import { GradleBuildDSL, TutorialModes } from '../../types';
import GitLabCITutorial, { GitLabCITutorialProps } from '../GitLabCITutorial';

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
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy to clipboard' })).toMatchSnapshot(
    'sonar token key',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy to clipboard' })).toMatchSnapshot(
    'sonarqube host url key',
  );
  expect(getCopyToClipboardHostURLValue({ i: 2, name: 'Copy to clipboard' })).toMatchSnapshot(
    'sonarqube host url value',
  );

  // Create/update configuration file step
  // Maven
  await user.click(ui.mavenBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot('Maven: pom.xml');
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot('Maven: gitlab-ci.yml');

  // Gradle
  await user.click(ui.gradleBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot('Groovy: build.gradle');
  await user.click(ui.gradleDSLButton(GradleBuildDSL.Kotlin).get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'Kotlin: build.gradle.kts',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot('Gradle: gitlab-ci.yml');

  // .NET
  await user.click(ui.dotnetBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot('.NET: gitlab-ci.yml');

  // C++/Objective-C
  await user.click(ui.cppBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'CPP: sonar-project.properties',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot('CPP: gitlab-ci.yml');

  // c++ manual config
  await user.click(ui.autoConfigManual.get());
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    'CPP - manual: gitlab-ci.yml',
  );

  // Dart
  await user.click(ui.dartBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'Dart: sonar-project.properties',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot('Dart: gitlab-ci.yml');

  // Other
  await user.click(ui.otherBuildButton.get());
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'Other: sonar-project.properties',
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot('Other: gitlab-ci.yml');

  expect(ui.allSetSentence.get()).toBeInTheDocument();
});

it('should generate/delete a new token or use existing one', async () => {
  const user = userEvent.setup();
  renderGitLabTutorial();

  expect(await ui.secretsStepTitle.find()).toBeInTheDocument();

  // Generate token
  await user.click(ui.genTokenDialogButton.get());
  await user.click(ui.generateTokenButton.get());
  expect(getCopyToClipboardValue({ inlineSnippet: true })).toEqual('generatedtoken2');

  // Revoke current token and create new one
  await user.click(ui.deleteTokenButton.get());
  await user.type(ui.tokenNameInput.get(), 'newtoken');
  await user.click(ui.expiresInSelect.get());
  await user.click(byRole('option', { name: 'users.tokens.expiration.365' }).get());

  await user.click(ui.generateTokenButton.get());
  expect(ui.tokenValue.get()).toBeInTheDocument();
  await user.click(ui.continueButton.getAll()[0]);
  expect(ui.tokenValue.query()).not.toBeInTheDocument();
});

function renderGitLabTutorial(
  overrides: Partial<GitLabCITutorialProps> = {},
  { languages = { c: mockLanguage({ key: 'c' }) } }: RenderContext = {},
) {
  return renderApp(
    '/',
    <GitLabCITutorial
      baseUrl="http://localhost:9000"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      {...overrides}
    />,
    { languages },
  );
}
