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
import { act, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { addGlobalErrorMessage, addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import { renderApp } from '../../../helpers/testReactTestingUtils';

function NullComponent() {
  return null;
}

it('should display messages', async () => {
  jest.useFakeTimers();

  // we render anything, the GlobalMessageContainer is rendered independently from routing
  renderApp('sonarqube', <NullComponent />);

  await waitFor(() => {
    addGlobalErrorMessage('This is an error');
    addGlobalSuccessMessage('This was a triumph!');
  });
  expect(await screen.findByRole('alert')).toHaveTextContent('This is an error');
  expect(screen.getByRole('status')).toHaveTextContent('This was a triumph!');

  // No duplicate message
  await waitFor(() => {
    addGlobalErrorMessage('This is an error');
  });
  expect(screen.getByRole('alert')).toHaveTextContent(/^This is an error$/);
  await waitFor(() => {
    addGlobalSuccessMessage('This was a triumph!');
  });
  expect(await screen.findByRole('status')).toHaveTextContent(
    /^This was a triumph!This was a triumph!$/,
  );

  act(() => {
    jest.runAllTimers();
  });
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(screen.queryByRole('status')).not.toBeInTheDocument();

  jest.useRealTimers();
});
