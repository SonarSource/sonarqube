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
import * as React from 'react';
import { mockHotspot } from '../../../../../helpers/mocks/security-hotspots';
import { mockCurrentUser } from '../../../../../helpers/testMocks';
import { Status, StatusProps } from '../Status';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { setSecurityHotspotStatus } from '../../../../../api/security-hotspots';
import { HotspotResolution, HotspotStatus } from '../../../../../types/security-hotspots';

jest.mock('../../../../../api/security-hotspots', () => ({
  setSecurityHotspotStatus: jest.fn().mockResolvedValue({}),
}));

it('should properly deal with comment/status/submit events', async () => {
  const hotspot = mockHotspot();
  renderStatusSelection({ hotspot });
  const user = userEvent.setup();
  const comment = 'COMMENT-TEXT';

  await user.click(screen.getByRole('button', { name: 'hotspots.status.select_status' }));

  await user.click(
    screen.getByRole('radio', {
      name: 'hotspots.status_option.SAFE hotspots.status_option.SAFE.description',
    })
  );

  await user.click(screen.getByRole('textbox'));
  await user.keyboard(comment);

  await user.click(screen.getByRole('button', { name: 'hotspots.status.change_status' }));

  expect(setSecurityHotspotStatus).toHaveBeenCalledWith(hotspot.key, {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.SAFE,
    comment,
  });
});

it('should open change status panel correctly', async () => {
  renderStatusSelection();
  const user = userEvent.setup();
  expect(screen.queryByTestId('security-hotspot-test')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'hotspots.status.select_status' }));
  expect(screen.getByTestId('security-hotspot-test')).toBeInTheDocument();
});

it('should disallow status change for hotspot that are readonly', () => {
  renderStatusSelection({ hotspot: mockHotspot({ canChangeStatus: false }) });
  expect(screen.getByRole('button')).toBeDisabled();
});

it('should disallow status change for user that are not logged in', () => {
  renderStatusSelection({ currentUser: mockCurrentUser({ isLoggedIn: false }) });
  expect(screen.getByRole('button')).toBeDisabled();
});

function renderStatusSelection(props?: Partial<StatusProps>) {
  render(
    <Status
      currentUser={mockCurrentUser({ isLoggedIn: true })}
      hotspot={mockHotspot()}
      onStatusChange={jest.fn()}
      {...props}
    />
  );
}
