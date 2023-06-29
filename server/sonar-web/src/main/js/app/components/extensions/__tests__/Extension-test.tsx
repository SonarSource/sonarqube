/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { lightTheme } from 'design-system';
import * as React from 'react';
import { getExtensionStart } from '../../../../helpers/extensions';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import Extension from '../Extension';

jest.mock('../../../../helpers/extensions', () => ({
  getExtensionStart: jest.fn().mockResolvedValue({}),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

const extensionView = <button type="button">Extension</button>;

it('should render React extensions correctly', async () => {
  const start = jest.fn().mockReturnValue(extensionView);
  jest.mocked(getExtensionStart).mockResolvedValue(start);

  const { rerender } = renderExtension();
  expect(await screen.findByRole('button', { name: 'Extension' })).toBeInTheDocument();
  expect(start).toHaveBeenCalled();

  rerender(<Extension extension={{ key: 'BAR', name: 'BAR' }} />);
  expect(screen.queryByRole('button', { name: 'Extension' })).not.toBeInTheDocument();
  expect(await screen.findByRole('button', { name: 'Extension' })).toBeInTheDocument();
});

it('should handle Function extensions correctly', async () => {
  const stop = jest.fn();
  const start = jest.fn(() => stop);
  jest.mocked(getExtensionStart).mockResolvedValue(start);

  const { rerender } = renderExtension();
  await new Promise(setImmediate);
  expect(start).toHaveBeenCalled();

  rerender(<Extension extension={{ key: 'BAR', name: 'BAR' }} />);

  expect(stop).toHaveBeenCalled();
});

function renderExtension(props: Partial<typeof Extension> = {}) {
  return renderComponent(
    <Extension theme={lightTheme} extension={{ key: 'foo', name: 'Foo' }} {...props} />
  );
}
