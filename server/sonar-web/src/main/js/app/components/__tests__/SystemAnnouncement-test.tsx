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
import { fireEvent, screen } from '@testing-library/dom';
import * as React from 'react';
import { getValues } from '../../../api/settings';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import SystemAnnouncement from '../SystemAnnouncement';

jest.mock('../../../api/settings', () => ({
  getValues: jest.fn()
}));

it('should display system announcement', async () => {
  (getValues as jest.Mock)
    .mockResolvedValueOnce([
      {
        key: 'sonar.systemAnnouncement.displayMessage',
        value: 'false',
        inherited: true
      }
    ])
    .mockResolvedValueOnce([
      {
        key: 'sonar.systemAnnouncement.displayMessage',
        value: 'false',
        inherited: true
      }
    ])
    .mockResolvedValueOnce([
      {
        key: 'sonar.systemAnnouncement.displayMessage',
        value: 'true'
      }
    ])
    .mockResolvedValueOnce([
      {
        key: 'sonar.systemAnnouncement.displayMessage',
        value: 'true'
      },
      {
        key: 'sonar.systemAnnouncement.message',
        value: ''
      }
    ])
    .mockResolvedValueOnce([
      {
        key: 'sonar.systemAnnouncement.displayMessage',
        value: 'true'
      },
      {
        key: 'sonar.systemAnnouncement.message',
        value: 'Foo'
      }
    ]);

  renderSystemAnnouncement();

  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  fireEvent(document, new Event('visibilitychange'));
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  fireEvent(document, new Event('visibilitychange'));
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  fireEvent(document, new Event('visibilitychange'));
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  fireEvent(document, new Event('visibilitychange'));
  expect(await screen.findByRole('alert')).toBeInTheDocument();
  expect(screen.getByText('Foo')).toBeInTheDocument();
});

function renderSystemAnnouncement() {
  return renderComponent(<SystemAnnouncement />);
}
