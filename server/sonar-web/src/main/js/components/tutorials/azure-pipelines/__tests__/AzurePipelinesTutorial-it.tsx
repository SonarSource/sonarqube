/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { UserEvent } from '@testing-library/user-event/dist/types/setup';
import * as React from 'react';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAppState, mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderApp, RenderContext } from '../../../../helpers/testReactTestingUtils';
import { Permissions } from '../../../../types/permissions';
import { TokenType } from '../../../../types/token';
import AzurePipelinesTutorial, { AzurePipelinesTutorialProps } from '../AzurePipelinesTutorial';

jest.mock('../../../../api/user-tokens');

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([])
}));

let tokenMock: UserTokensMock;

beforeAll(() => {
  tokenMock = new UserTokensMock();
});

afterEach(() => {
  tokenMock.reset();
});

it('should render correctly and allow navigating between the different steps', async () => {
  renderAzurePipelinesTutorial();
  const user = userEvent.setup();

  expect(
    screen.getByRole('heading', { name: 'onboarding.tutorial.with.azure_pipelines.title' })
  ).toBeInTheDocument();

  //// Default step.
  assertDefaultStepIsCorrectlyRendered();

  // Continue.
  await goToNextStep(user);

  //// Token step.
  assertServiceEndpointStepIsCorrectlyRendered();

  // Generate a token.
  await clickButton(user, 'onboarding.token.generate.long');
  const modal = screen.getByRole('dialog');
  await clickButton(user, 'onboarding.token.generate', modal);
  const lastToken = tokenMock.getLastToken();
  if (lastToken === undefined) {
    throw new Error("Couldn't find the latest generated token.");
  }
  expect(lastToken.type).toBe(TokenType.Global);
  expect(within(modal).getByRole('alert')).toHaveTextContent(
    `users.tokens.new_token_created.${lastToken.token}`
  );
  await clickButton(user, 'continue', modal);

  // Continue.
  await goToNextStep(user);

  //// Analysis step: .NET
  await clickButton(user, 'onboarding.build.dotnet');
  assertDotNetStepIsCorrectlyRendered();

  //// Analysis step: Maven
  await clickButton(user, 'onboarding.build.maven');
  assertMavenStepIsCorrectlyRendered();

  //// Analysis step: Gradle
  await clickButton(user, 'onboarding.build.gradle');
  assertGradleStepIsCorrectlyRendered();

  //// Analysis step: Gradle
  await clickButton(user, 'onboarding.build.gradle');
  assertGradleStepIsCorrectlyRendered();

  //// Analysis step: C Family
  await clickButton(user, 'onboarding.build.cfamily');

  // OS: Linux
  await clickButton(user, 'onboarding.build.other.os.linux');
  assertCFamilyLinuxStepIsCorrectlyRendered();

  // OS: Windows
  await clickButton(user, 'onboarding.build.other.os.win');
  assertCFamilyWindowsStepIsCorrectlyRendered();

  // OS: macOS
  await clickButton(user, 'onboarding.build.other.os.mac');
  assertCFamilyMacOSStepIsCorrectlyRendered();

  //// Analysis step: Other
  await clickButton(user, 'onboarding.build.other');
  assertOtherStepIsCorrectlyRendered();

  //// Finish tutorial
  await clickButton(user, 'tutorials.finish');
  assertFinishStepIsCorrectlyRendered();
});

it('allows to navigate back to a previous step', async () => {
  renderAzurePipelinesTutorial();
  const user = userEvent.setup();

  // No clickable steps.
  expect(
    screen.queryByRole('button', {
      name: '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title'
    })
  ).not.toBeInTheDocument();

  // Go to the next steps.
  await goToNextStep(user);
  await goToNextStep(user);

  // The first 2 steps become clickable.
  expect(
    screen.getByRole('button', {
      name: '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title'
    })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', {
      name: '2 onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.title'
    })
  ).toBeInTheDocument();

  // Navigate back to the first step.
  await clickButton(user, '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title');

  // No more clickable steps.
  expect(
    screen.queryByRole('button', {
      name: '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title'
    })
  ).not.toBeInTheDocument();
});

it('should not offer CFamily analysis if the language is not available', async () => {
  renderAzurePipelinesTutorial(undefined, { languages: {} });
  const user = userEvent.setup();

  // Go to the analysis step.
  await goToNextStep(user);
  await goToNextStep(user);

  expect(screen.getByRole('button', { name: 'onboarding.build.dotnet' })).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'onboarding.build.cfamily' })
  ).not.toBeInTheDocument();
});

function renderAzurePipelinesTutorial(
  props: Partial<AzurePipelinesTutorialProps> = {},
  {
    appState = mockAppState({ branchesEnabled: true }),
    languages = { c: mockLanguage({ key: 'c' }) }
  }: RenderContext = {}
) {
  return renderApp(
    '/',
    <AzurePipelinesTutorial
      baseUrl="http://localhost:9000"
      component={mockComponent()}
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Scan] } })}
      willRefreshAutomatically={true}
      {...props}
    />,
    { appState, languages }
  );
}

async function clickButton(user: UserEvent, name: string, context?: HTMLElement) {
  if (context) {
    await user.click(within(context).getByRole('button', { name }));
  } else {
    await user.click(screen.getByRole('button', { name }));
  }
}

async function goToNextStep(user: UserEvent) {
  await clickButton(user, 'continue');
}

function assertDefaultStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title'
    })
  ).toBeInTheDocument();
  expect(screen.getByTestId('azure-tutorial__extension')).toMatchSnapshot('extension step');
}

function assertServiceEndpointStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.title'
    })
  ).toBeInTheDocument();
  expect(screen.getByTestId('azure-tutorial__service-endpoint')).toMatchSnapshot(
    'service endpoint step'
  );
  expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'onboarding.token.generate.long' })
  ).toBeInTheDocument();
}

function assertDotNetStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.title'
    })
  ).toBeInTheDocument();

  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot('dotnet step');
  expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
}

function assertMavenStepIsCorrectlyRendered() {
  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot('maven step');
  expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
}

function assertGradleStepIsCorrectlyRendered() {
  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot('gradle step');
  expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
}

function assertCFamilyLinuxStepIsCorrectlyRendered() {
  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot(
    'cfamily linux step'
  );
  expect(screen.getAllByRole('button', { name: 'copy_to_clipboard' })).toHaveLength(4);
}

function assertCFamilyWindowsStepIsCorrectlyRendered() {
  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot(
    'cfamily windows step'
  );
  expect(screen.getAllByRole('button', { name: 'copy_to_clipboard' })).toHaveLength(4);
}

function assertCFamilyMacOSStepIsCorrectlyRendered() {
  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot(
    'cfamily macos step'
  );
  expect(screen.getAllByRole('button', { name: 'copy_to_clipboard' })).toHaveLength(4);
}

function assertOtherStepIsCorrectlyRendered() {
  expect(screen.getByTestId('azure-tutorial__analysis-command')).toMatchSnapshot('other step');
  expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
}

function assertFinishStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.ci_outro.all_set.title'
    })
  ).toBeInTheDocument();
  expect(screen.getByTestId('azure-tutorial__all-set')).toMatchSnapshot('all set step');
}
