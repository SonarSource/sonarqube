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
import * as React from 'react';
import { isValidLicense } from '../../../../api/editions';
import { mockTask } from '../../../../helpers/mocks/tasks';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { AnalysisLicenseError } from '../AnalysisLicenseError';

jest.mock('../../../../api/editions', () => ({
  isValidLicense: jest.fn().mockResolvedValue({ isValidLicense: true }),
}));

it('should handle a valid license', async () => {
  renderAnalysisLicenseError({
    currentTask: mockTask({ errorType: 'ANY_TYPE' }),
  });

  expect(
    await screen.findByText(
      'component_navigation.status.last_blocked_due_to_bad_license_X.qualifier.TRK',
    ),
  ).toBeInTheDocument();
});

it('should send user to contact the admin', async () => {
  const errorMessage = 'error message';
  renderAnalysisLicenseError({
    currentTask: mockTask({ errorMessage, errorType: 'LICENSING_LOC' }),
  });

  expect(await screen.findByText('please_contact_administrator')).toBeInTheDocument();
  expect(screen.getByText(errorMessage)).toBeInTheDocument();
});

it('should send provide a link to the admin', async () => {
  jest.mocked(isValidLicense).mockResolvedValueOnce({ isValidLicense: false });

  const errorMessage = 'error message';
  renderAnalysisLicenseError(
    {
      currentTask: mockTask({ errorMessage, errorType: 'error-type' }),
    },
    true,
  );

  expect(
    await screen.findByText('license.component_navigation.button.error-type.'),
  ).toBeInTheDocument();
  expect(screen.getByText(errorMessage)).toBeInTheDocument();
});

function renderAnalysisLicenseError(
  overrides: Partial<Parameters<typeof AnalysisLicenseError>[0]> = {},
  canAdmin = false,
) {
  return renderApp('/', <AnalysisLicenseError currentTask={mockTask()} {...overrides} />, {
    appState: mockAppState({ canAdmin }),
  });
}
