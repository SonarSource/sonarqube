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

import { screen } from '@testing-library/react';

import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import DopTranslationServiceMock from '../../../../api/mocks/DopTranslationServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import CreateProjectPage from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let dopTranslationHandler: DopTranslationServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;

const original = window.location;

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  dopTranslationHandler = new DopTranslationServiceMock();
  newCodePeriodHandler = new NewCodeDefinitionServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almIntegrationHandler.reset();
  dopTranslationHandler.reset();
  newCodePeriodHandler.reset();
});
afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: original });
});

it('should be able to setup if no config and admin', async () => {
  dopTranslationHandler.removeDopTypeFromSettings(AlmKeys.Azure);
  renderCreateProject(true);
  expect(await screen.findByText('onboarding.create_project.select_method')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'setup' })).toBeInTheDocument();
});

it('should not be able to setup if no config and no admin rights', async () => {
  dopTranslationHandler.removeDopTypeFromSettings(AlmKeys.Azure);
  renderCreateProject();
  expect(await screen.findByText('onboarding.create_project.select_method')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'setup' })).not.toBeInTheDocument();
  await expect(screen.getByLabelText('help-tooltip')).toHaveATooltipWithContent(
    'onboarding.create_project.alm_not_configured',
  );
});

it('should be able to setup if config is present', async () => {
  renderCreateProject();
  expect(await screen.findByText('onboarding.create_project.select_method')).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'onboarding.create_project.import_select_method.bitbucket' }),
  ).toBeInTheDocument();
});

function renderCreateProject(canAdmin: boolean = false) {
  renderApp('project/create', <CreateProjectPage />, {
    appState: mockAppState({ canAdmin }),
  });
}
