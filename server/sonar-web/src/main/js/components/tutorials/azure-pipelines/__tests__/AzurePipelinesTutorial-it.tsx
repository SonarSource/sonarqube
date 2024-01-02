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
import * as React from 'react';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderApp, RenderContext } from '../../../../helpers/testReactTestingUtils';
import { Permissions } from '../../../../types/permissions';
import { TokenType } from '../../../../types/token';
import { OSs } from '../../types';
import AzurePipelinesTutorial, { AzurePipelinesTutorialProps } from '../AzurePipelinesTutorial';

jest.mock('../../../../api/user-tokens');

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

  //// Analysis step: C Family
  await clickButton(user, 'onboarding.build.cfamily');

  // OS's
  await clickButton(user, `onboarding.build.other.os.${OSs.Linux}`);
  assertCFamilyStepIsCorrectlyRendered(OSs.Linux);

  await clickButton(user, `onboarding.build.other.os.${OSs.Windows}`);
  assertCFamilyStepIsCorrectlyRendered(OSs.Windows);

  await clickButton(user, `onboarding.build.other.os.${OSs.MacOS}`);
  assertCFamilyStepIsCorrectlyRendered(OSs.MacOS);

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
      name: '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title',
    })
  ).not.toBeInTheDocument();

  // Go to the next steps.
  await goToNextStep(user);
  await goToNextStep(user);

  // The first 2 steps become clickable.
  expect(
    screen.getByRole('button', {
      name: '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title',
    })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', {
      name: '2 onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.title',
    })
  ).toBeInTheDocument();

  // Navigate back to the first step.
  await clickButton(user, '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title');

  // No more clickable steps.
  expect(
    screen.queryByRole('button', {
      name: '1 onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title',
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

function assertDefaultStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.ExtensionInstallation.title',
    })
  ).toBeInTheDocument();
}

function assertServiceEndpointStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.title',
    })
  ).toBeInTheDocument();
  expect(getCopyToClipboardValue()).toBe('https://sonarqube.example.com/');
  expect(
    screen.getByRole('button', { name: 'onboarding.token.generate.long' })
  ).toBeInTheDocument();
}

function assertDotNetStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.title',
    })
  ).toBeInTheDocument();
  expect(getCopyToClipboardValue()).toBe('foo');
}

function assertMavenStepIsCorrectlyRendered() {
  expect(getCopyToClipboardValue()).toMatchSnapshot('maven, copy additional properties');
}

function assertGradleStepIsCorrectlyRendered() {
  expect(getCopyToClipboardValue()).toMatchSnapshot('gradle, copy additional properties');
}

function assertCFamilyStepIsCorrectlyRendered(os: string) {
  expect(getCopyToClipboardValue(0)).toMatchSnapshot(`cfamily ${os}, copy shell script`);
  expect(getCopyToClipboardValue(1)).toBe('foo');
  expect(getCopyToClipboardValue(2)).toMatchSnapshot(`cfamily ${os}, copy additional properties`);
  expect(getCopyToClipboardValue(3)).toMatchSnapshot(`cfamily ${os}, copy build-wrapper command`);
}

function assertOtherStepIsCorrectlyRendered() {
  expect(getCopyToClipboardValue()).toBe('foo');
}

function assertFinishStepIsCorrectlyRendered() {
  expect(
    screen.getByRole('heading', {
      name: 'onboarding.tutorial.ci_outro.all_set.title',
    })
  ).toBeInTheDocument();
}

function renderAzurePipelinesTutorial(
  props: Partial<AzurePipelinesTutorialProps> = {},
  { languages = { c: mockLanguage({ key: 'c' }) } }: RenderContext = {}
) {
  return renderApp(
    '/',
    <AzurePipelinesTutorial
      baseUrl="https://sonarqube.example.com/"
      component={mockComponent({ key: 'foo' })}
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Scan] } })}
      willRefreshAutomatically={true}
      {...props}
    />,
    { languages }
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

function getCopyToClipboardValue(i = 0, name = 'copy_to_clipboard') {
  return screen.getAllByRole('button', { name })[i].getAttribute('data-clipboard-text');
}
