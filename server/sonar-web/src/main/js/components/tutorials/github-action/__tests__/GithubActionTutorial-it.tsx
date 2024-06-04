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
import selectEvent from 'react-select-event';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { RenderContext, renderApp } from '../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import {
  getCommonNodes,
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import { GradleBuildDSL, TutorialModes } from '../../types';
import GitHubActionTutorial, { GitHubActionTutorialProps } from '../GitHubActionTutorial';

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

const tokenMock = new UserTokensMock();
const almMock = new AlmSettingsServiceMock();

afterEach(() => {
  tokenMock.reset();
  almMock.reset();
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
  expect(getCopyToClipboardValue(0, 'Copy to clipboard')).toMatchSnapshot('sonar token key');
  expect(getCopyToClipboardValue(1, 'Copy to clipboard')).toMatchSnapshot('sonarqube host url key');
  expect(getCopyToClipboardValue(2, 'Copy to clipboard')).toMatchSnapshot(
    'sonarqube host url value',
  );

  // Create/update configuration file step
  // Maven
  await user.click(ui.mavenBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('Maven: .github/workflows/build.yml');

  // Gradle
  await user.click(ui.gradleBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('Groovy: build.gradle');
  await user.click(ui.gradleDSLButton(GradleBuildDSL.Kotlin).get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('Kotlin: build.gradle.kts');
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot('Gradle: .github/workflows/build.yml');

  // .NET
  await user.click(ui.dotnetBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('.NET: .github/workflows/build.yml');

  // Cpp
  await user.click(ui.cppBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'C++ (automatic) and other: sonar-project.properties',
  );
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'C++ (automatic) and other: .github/workflows/build.yml',
  );

  await user.click(ui.autoConfigManual.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot('C++: sonar-project.properties');
  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'C++ Linux: .github/workflows/build.yml',
  );
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'C++ Windows: .github/workflows/build.yml',
  );
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'C++ MacOS: .github/workflows/build.yml',
  );

  // Objective-C
  await user.click(ui.objCBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'Objective-C: sonar-project.properties',
  );

  await user.click(ui.linuxButton.get());
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'Objective-C Linux: .github/workflows/build.yml',
  );
  await user.click(ui.windowsButton.get());
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'Objective-C Windows: .github/workflows/build.yml',
  );
  await user.click(ui.macosButton.get());
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'Objective-C MacOS: .github/workflows/build.yml',
  );

  // Other
  await user.click(ui.otherBuildButton.get());
  expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(
    'C++ (automatic) and other: sonar-project.properties',
  );
  expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(
    'C++ (automatic) and other: .github/workflows/build.yml',
  );

  expect(ui.allSetSentence.get()).toBeInTheDocument();
});

it('should generate/delete a new token or use existing one', async () => {
  const user = userEvent.setup();
  renderGithubActionTutorial();

  expect(await ui.secretsStepTitle.find()).toBeInTheDocument();

  // Generate token
  await user.click(ui.genTokenDialogButton.get());
  await user.click(ui.generateTokenButton.get());
  expect(getCopyToClipboardValue()).toEqual('generatedtoken2');

  // Revoke current token and create new one
  await user.click(ui.deleteTokenButton.get());
  await user.type(ui.tokenNameInput.get(), 'newtoken');
  await selectEvent.select(ui.expiresInSelect.get(), 'users.tokens.expiration.365');
  await user.click(ui.generateTokenButton.get());
  expect(ui.tokenValue.get()).toBeInTheDocument();
  await user.click(ui.continueButton.getAll()[0]);
  expect(ui.tokenValue.query()).not.toBeInTheDocument();
});

it('navigates between steps', async () => {
  const user = userEvent.setup();
  almMock.handleSetProjectBinding(AlmKeys.GitHub, {
    almSetting: 'my-project',
    project: 'my-project',
    repository: 'my-project',
    monorepo: true,
  });
  renderGithubActionTutorial({
    almBinding: mockAlmSettingsInstance({
      alm: AlmKeys.GitHub,
      url: 'http://localhost/qube',
    }),
  });

  // If project is bound, link to repo is visible
  expect(await ui.linkToRepo.find()).toBeInTheDocument();

  await user.click(ui.mavenBuildButton.get());
  expect(ui.allSetSentence.get()).toBeInTheDocument();

  await user.click(ui.ymlFileStepTitle.get());
  expect(ui.mavenBuildButton.get()).toBeInTheDocument();
  await user.click(ui.secretsStepTitle.get());
  expect(ui.genTokenDialogButton.get()).toBeInTheDocument();
});

function renderGithubActionTutorial(
  overrides: Partial<GitHubActionTutorialProps> = {},
  { languages = { c: mockLanguage({ key: 'c' }) } }: RenderContext = {},
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
    { languages, featureList: [Feature.BranchSupport] },
  );
}
