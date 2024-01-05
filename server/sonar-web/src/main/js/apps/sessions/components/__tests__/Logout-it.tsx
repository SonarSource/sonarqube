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
import * as React from 'react';
import { logOut } from '../../../../api/auth';
import RecentHistory from '../../../../app/components/RecentHistory';
import { addGlobalErrorMessage } from '../../../../helpers/globalMessages';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import Logout from '../Logout';

jest.mock('../../../../api/auth', () => ({
  logOut: jest.fn().mockResolvedValue(true),
}));

jest.mock('../../../../helpers/globalMessages', () => ({
  addGlobalErrorMessage: jest.fn(),
}));

jest.mock('../../../../helpers/system', () => ({
  getBaseUrl: jest.fn().mockReturnValue('/context'),
}));

jest.mock('../../../../app/components/RecentHistory', () => ({
  __esModule: true,
  default: {
    clear: jest.fn(),
  },
}));

const originalLocation = window.location;
const replace = jest.fn();

beforeAll(() => {
  const location = {
    ...window.location,
    replace,
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

beforeEach(jest.clearAllMocks);

it('should behave correctly', async () => {
  renderLogout();

  expect(screen.getByText('logging_out')).toBeInTheDocument();
  await waitFor(() => {
    expect(replace).toHaveBeenCalledWith('/context/');
  });
  expect(RecentHistory.clear).toHaveBeenCalled();
});

it('should correctly handle a failing log out', async () => {
  jest.mocked(logOut).mockRejectedValueOnce(false);
  renderLogout();

  await waitFor(() => {
    expect(addGlobalErrorMessage).toHaveBeenCalledWith('login.logout_failed');
  });
});

function renderLogout() {
  return renderComponent(<Logout />);
}
