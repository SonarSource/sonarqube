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
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockTask } from '../../../../helpers/mocks/tasks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { AnalysisErrorModal } from '../AnalysisErrorModal';

jest.mock('../AnalysisErrorMessage', () => ({
  AnalysisErrorMessage: () => <div>analysis error message</div>,
}));

jest.mock('../AnalysisLicenseError', () => ({
  AnalysisLicenseError: () => <div>analysis license error</div>,
}));

it('should show the license error message', () => {
  renderAnalysisErrorModal({
    currentTask: mockTask({ errorType: 'ANY_TYPE' }),
  });

  expect(screen.getByText('error')).toBeInTheDocument();
  expect(screen.queryByText('analysis error message')).not.toBeInTheDocument();
  expect(screen.getByText('analysis license error')).toBeInTheDocument();
});

it('should show the analysis error message', () => {
  renderAnalysisErrorModal();

  expect(screen.getByText('error')).toBeInTheDocument();
  expect(screen.queryByText('analysis license error')).not.toBeInTheDocument();
  expect(screen.getByText('analysis error message')).toBeInTheDocument();
});

function renderAnalysisErrorModal(
  overrides: Partial<Parameters<typeof AnalysisErrorModal>[0]> = {},
  location = '/',
) {
  return renderApp(
    location,
    <AnalysisErrorModal
      component={mockComponent()}
      currentTask={mockTask()}
      onClose={jest.fn()}
      {...overrides}
    />,
  );
}
