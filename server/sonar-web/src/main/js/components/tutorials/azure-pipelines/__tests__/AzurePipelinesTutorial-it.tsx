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

import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { RenderContext, renderApp } from '../../../../helpers/testReactTestingUtils';
import { Permissions } from '../../../../types/permissions';
import { TokenType } from '../../../../types/token';
import { getCopyToClipboardValue, getTutorialBuildButtons } from '../../test-utils';
import { OSs } from '../../types';
import AzurePipelinesTutorial, { AzurePipelinesTutorialProps } from '../AzurePipelinesTutorial';

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

let tokenMock: UserTokensMock;

beforeAll(() => {
  tokenMock = new UserTokensMock();
});

afterEach(() => {
  tokenMock.reset();
});

it('should render correctly and allow token generation', async () => {
  renderAzurePipelinesTutorial();
  const user = userEvent.setup();

  expect(
    screen.getByRole('heading', { name: 'onboarding.tutorial.with.azure_pipelines.title' }),
  ).toBeInTheDocument();

  // Default step.
  assertDefaultStepIsCorrectlyRendered();

  // Token step.
  assertServiceEndpointStepIsCorrectlyRendered();

  // Generate a token.
  await clickButton(user, 'onboarding.token.generate.long');
  const modal = screen.getByRole('dialog');
  await clickButton(user, 'onboarding.token.generate', modal);
  const lastToken = tokenMock.getLastToken();

  expect(lastToken).toBeDefined();

  expect(lastToken!.type).toBe(TokenType.Global);
  expect(
    within(modal).getByText(`users.tokens.new_token_created.${lastToken!.token}`),
  ).toBeInTheDocument();
  await clickButton(user, 'continue', modal);

  // Analysis step: .NET
  await user.click(getTutorialBuildButtons().dotnetBuildButton.get());
  assertDotNetStepIsCorrectlyRendered();

  // Analysis step: Maven
  await user.click(getTutorialBuildButtons().mavenBuildButton.get());
  assertMavenStepIsCorrectlyRendered();

  // Analysis step: Gradle
  await user.click(getTutorialBuildButtons().gradleBuildButton.get());
  assertGradleStepIsCorrectlyRendered();

  // Analysis step: C Family
  await user.click(getTutorialBuildButtons().cppBuildButton.get());
  // Default: Automatic configuration
  // expect linux/win/macos buttons not to be present
  expect(getTutorialBuildButtons().linuxButton.query()).not.toBeInTheDocument();
  expect(getTutorialBuildButtons().windowsButton.query()).not.toBeInTheDocument();
  expect(getTutorialBuildButtons().macosButton.query()).not.toBeInTheDocument();
  assertAutomaticCppStepIsCorrectlyRendered();

  // Switch to manual configuration
  await user.click(getTutorialBuildButtons().autoConfigManual.get());
  await user.click(getTutorialBuildButtons().linuxButton.get());
  assertManualCppStepIsCorrectlyRendered(OSs.Linux);
  await user.click(getTutorialBuildButtons().arm64Button.get());
  assertManualCppStepIsCorrectlyRendered(OSs.Linux, 'arm64');
  await user.click(getTutorialBuildButtons().windowsButton.get());
  assertObjCStepIsCorrectlyRendered(OSs.Windows);
  await user.click(getTutorialBuildButtons().macosButton.get());
  assertObjCStepIsCorrectlyRendered(OSs.MacOS);

  // Analysis step: C Family
  await user.click(getTutorialBuildButtons().objCBuildButton.get());
  await user.click(getTutorialBuildButtons().linuxButton.get());
  await user.click(getTutorialBuildButtons().x86_64Button.get());
  assertObjCStepIsCorrectlyRendered(OSs.Linux);
  await user.click(getTutorialBuildButtons().arm64Button.get());
  assertObjCStepIsCorrectlyRendered(OSs.Linux, 'arm64');
  await user.click(getTutorialBuildButtons().windowsButton.get());
  assertObjCStepIsCorrectlyRendered(OSs.Windows);
  await user.click(getTutorialBuildButtons().macosButton.get());
  assertObjCStepIsCorrectlyRendered(OSs.MacOS);

  // Analysis step: Dart
  await user.click(getTutorialBuildButtons().dartBuildButton.get());
  assertOtherStepIsCorrectlyRendered();

  // Analysis step: Other
  await user.click(getTutorialBuildButtons().otherBuildButton.get());
  assertOtherStepIsCorrectlyRendered();

  // Finish tutorial
  assertFinishStepIsCorrectlyRendered();
});

it('should not offer CFamily analysis if the language is not available', () => {
  renderAzurePipelinesTutorial(undefined, { languages: {} });

  expect(getTutorialBuildButtons().dotnetBuildButton.get()).toBeInTheDocument();
  expect(getTutorialBuildButtons().cppBuildButton.query()).not.toBeInTheDocument();
});

function assertDefaultStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title',
    }),
  ).toBeInTheDocument();
}

function assertServiceEndpointStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.title',
    }),
  ).toBeInTheDocument();
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy to clipboard', inlineSnippet: true })).toBe(
    'https://sonarqube.example.com/',
  );
  expect(
    screen.getByRole('button', { name: 'onboarding.token.generate.long' }),
  ).toBeInTheDocument();
}

function assertDotNetStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.title',
    }),
  ).toBeInTheDocument();

  expect(getCopyToClipboardValue({ i: 1, name: 'Copy to clipboard', inlineSnippet: true })).toBe(
    'foo',
  );
}

function assertMavenStepIsCorrectlyRendered() {
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'maven, copy additional properties',
  );
}

function assertGradleStepIsCorrectlyRendered() {
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    'gradle, copy additional properties',
  );
}

function assertObjCStepIsCorrectlyRendered(os: string, arch: string = 'x86_64') {
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    `objectivec ${os} ${arch}, copy shell script`,
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy to clipboard', inlineSnippet: true })).toBe(
    'foo',
  );
  expect(
    getCopyToClipboardValue({ i: 2, name: 'Copy to clipboard', inlineSnippet: true }),
  ).toMatchSnapshot(`objectivec ${os} ${arch}, copy additional properties`);
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    `objectivec ${os} ${arch}, copy build-wrapper command`,
  );
}

function assertAutomaticCppStepIsCorrectlyRendered() {
  assertOtherStepIsCorrectlyRendered();
}

function assertManualCppStepIsCorrectlyRendered(os: string, arch: string = 'x86_64') {
  expect(getCopyToClipboardValue({ i: 0, name: 'Copy' })).toMatchSnapshot(
    `manual-cpp ${os} ${arch}, copy shell script`,
  );
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy to clipboard', inlineSnippet: true })).toBe(
    'foo',
  );
  expect(
    getCopyToClipboardValue({ i: 2, name: 'Copy to clipboard', inlineSnippet: true }),
  ).toMatchSnapshot(`manual-cpp ${os} ${arch}, copy additional properties`);
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy' })).toMatchSnapshot(
    `manual-cpp ${os} ${arch}, copy build-wrapper command`,
  );
}

function assertOtherStepIsCorrectlyRendered() {
  expect(getCopyToClipboardValue({ i: 1, name: 'Copy to clipboard', inlineSnippet: true })).toBe(
    'foo',
  );
}

function assertFinishStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.ci_outro.done',
    }),
  ).toBeInTheDocument();
}

function renderAzurePipelinesTutorial(
  props: Partial<AzurePipelinesTutorialProps> = {},
  { languages = { c: mockLanguage({ key: 'c' }) } }: RenderContext = {},
) {
  return renderApp(
    '/',
    <AzurePipelinesTutorial
      baseUrl="https://sonarqube.example.com/"
      component={mockComponent({ key: 'foo' })}
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Scan] } })}
      willRefreshAutomatically
      {...props}
    />,
    { languages },
  );
}

async function clickButton(user: UserEvent, name: string, context?: HTMLElement) {
  if (context) {
    await user.click(within(context).getByRole('button', { name }));
  } else {
    await user.click(screen.getByRole('button', { name }));
  }
}
