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

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { addGlobalSuccessMessage } from '~design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import {
  getReportStatus,
  subscribeToEmailReport,
  unsubscribeFromEmailReport,
} from '../../../api/component-report';
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockComponentReportStatus } from '../../../helpers/mocks/component-report';
import { mockAppState, mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { ComponentReportActions } from '../ComponentReportActions';

jest.mock('~design-system', () => ({
  ...jest.requireActual('~design-system'),
  addGlobalSuccessMessage: jest.fn(),
}));

jest.mock('../../../api/component-report', () => ({
  ...jest.requireActual('../../../api/component-report'),
  getReportStatus: jest
    .fn()
    .mockResolvedValue(
      jest.requireActual('../../../helpers/mocks/component-report').mockComponentReportStatus(),
    ),
  subscribeToEmailReport: jest.fn().mockResolvedValue(undefined),
  unsubscribeFromEmailReport: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../../../helpers/system', () => ({
  ...jest.requireActual('../../../helpers/system'),
  getBaseUrl: jest.fn().mockReturnValue('baseUrl'),
}));

beforeEach(jest.clearAllMocks);

it('should not render anything when no status', async () => {
  jest.mocked(getReportStatus).mockRejectedValueOnce('Nope');

  renderComponentReportActions();

  // Loading
  expect(screen.queryByRole('button')).not.toBeInTheDocument();

  await waitFor(() => expect(getReportStatus).toHaveBeenCalled());

  // No status
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('should not render anything when branch is purgeable', async () => {
  renderComponentReportActions({
    branch: mockBranch({ excludedFromPurge: false }),
  });

  await waitFor(() => expect(getReportStatus).toHaveBeenCalled());

  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('should not render anything without governance', () => {
  renderComponentReportActions({ appState: mockAppState({ qualifiers: [] }) });

  expect(getReportStatus).not.toHaveBeenCalled();

  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('should allow user to (un)subscribe', async () => {
  jest
    .mocked(getReportStatus)
    .mockResolvedValueOnce(mockComponentReportStatus({ globalFrequency: 'monthly' }))
    .mockResolvedValueOnce(
      mockComponentReportStatus({ subscribed: true, globalFrequency: 'monthly' }),
    );

  const user = userEvent.setup();
  const component = mockComponent();
  const branch = mockBranch();

  renderComponentReportActions({
    component,
    branch,
    currentUser: mockLoggedInUser({ email: 'igot@nEmail.address' }),
  });

  expect(getReportStatus).toHaveBeenCalledWith(component.key, branch.name);

  const button = await screen.findByRole('button', {
    name: 'component_report.report.qualifier.TRK',
  });
  expect(button).toBeInTheDocument();
  await user.click(button);

  expect(screen.getByText('download_verb')).toBeInTheDocument();

  // Subscribe!
  const subscribeButton = screen.getByText('component_report.subscribe_x.report.frequency.monthly');
  expect(subscribeButton).toBeInTheDocument();

  await user.click(subscribeButton);

  expect(subscribeToEmailReport).toHaveBeenCalledWith(component.key, branch.name);
  expect(addGlobalSuccessMessage).toHaveBeenLastCalledWith(
    'component_report.subscribe_x_success.report.frequency.monthly.qualifier.trk',
  );

  // And unsubscribe!
  await user.click(button);

  const unsubscribeButton = screen.getByText(
    'component_report.unsubscribe_x.report.frequency.monthly',
  );
  expect(unsubscribeButton).toBeInTheDocument();

  await user.click(unsubscribeButton);

  expect(unsubscribeFromEmailReport).toHaveBeenCalledWith(component.key, branch.name);
  expect(addGlobalSuccessMessage).toHaveBeenLastCalledWith(
    'component_report.unsubscribe_x_success.report.frequency.monthly.qualifier.trk',
  );
});

it('should prevent user to subscribe if no email', async () => {
  const user = userEvent.setup();

  renderComponentReportActions({ currentUser: mockLoggedInUser({ email: undefined }) });

  await user.click(
    await screen.findByRole('button', {
      name: 'component_report.report.qualifier.TRK',
    }),
  );

  const subscribeButton = screen.getByText('component_report.no_email_to_subscribe');
  expect(subscribeButton).toBeInTheDocument();
  expect(subscribeButton).toBeDisabled();
});

function renderComponentReportActions(props: Partial<ComponentReportActions['props']> = {}) {
  return renderApp(
    '/',
    <ComponentReportActions
      appState={mockAppState({ qualifiers: [ComponentQualifier.Portfolio] })}
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      {...props}
    />,
  );
}
