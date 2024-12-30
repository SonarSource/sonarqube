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
import userEvent from '@testing-library/user-event';
import { ModeServiceMock } from '../../../api/mocks/ModeServiceMock';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { mockCurrentUser } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { Mode } from '../../../types/mode';
import { NoticeType } from '../../../types/users';
import ModeBanner from '../ModeBanner';

const modeHandler = new ModeServiceMock();
const usersHandler = new UsersServiceMock();

afterEach(() => {
  modeHandler.reset();
  usersHandler.reset();
});

describe('facetBanner', () => {
  it('renders as facetBanner for admins in MQR', async () => {
    const user = userEvent.setup();
    modeHandler.setMode(Mode.MQR);
    renderModeBanner(
      { as: 'facetBanner' },
      mockCurrentUser({ permissions: { global: ['admin'] } }),
    );
    expect(await screen.findByText('mode.mqr.advertisement')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'dismiss' }));
    expect(screen.queryByText('mode.mqr.advertisement')).not.toBeInTheDocument();
  });

  it('renders as facetBanner for admins in Standard', async () => {
    const user = userEvent.setup();
    modeHandler.setMode(Mode.Standard);
    renderModeBanner(
      { as: 'facetBanner' },
      mockCurrentUser({ permissions: { global: ['admin'] } }),
    );
    expect(await screen.findByText('mode.standard.advertisement')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'dismiss' }));
    expect(screen.queryByText('mode.standard.advertisement')).not.toBeInTheDocument();
  });

  it('does not render as facetBanner for regular users', () => {
    renderModeBanner({ as: 'facetBanner' }, mockCurrentUser());
    expect(screen.queryByText(/advertisement/)).not.toBeInTheDocument();
  });

  it('does not render if already dismissed', () => {
    renderModeBanner(
      { as: 'facetBanner' },
      mockCurrentUser({
        permissions: { global: ['admin'] },
        dismissedNotices: { [NoticeType.MQR_MODE_ADVERTISEMENT_BANNER]: true },
      }),
    );
    expect(screen.queryByText(/advertisement/)).not.toBeInTheDocument();
  });

  it('does not render if modified', () => {
    modeHandler.setModified();
    renderModeBanner(
      { as: 'facetBanner' },
      mockCurrentUser({
        permissions: { global: ['admin'] },
      }),
    );
    expect(screen.queryByText(/advertisement/)).not.toBeInTheDocument();
  });
});

describe('flagMessage', () => {
  it('renders as flagMessage for admins in MQR', async () => {
    const user = userEvent.setup();
    modeHandler.setMode(Mode.MQR);
    renderModeBanner({ as: 'wideBanner' }, mockCurrentUser({ permissions: { global: ['admin'] } }));
    expect(await screen.findByText('settings.mode.mqr.advertisement')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'dismiss' }));
    expect(screen.queryByText('settings.mode.mqr.advertisement')).not.toBeInTheDocument();
  });

  it('renders as flagMessage for admins in Standard', async () => {
    const user = userEvent.setup();
    modeHandler.setMode(Mode.Standard);
    renderModeBanner({ as: 'wideBanner' }, mockCurrentUser({ permissions: { global: ['admin'] } }));
    expect(await screen.findByText('settings.mode.standard.advertisement')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'dismiss' }));
    expect(screen.queryByText('settings.mode.standard.advertisement')).not.toBeInTheDocument();
  });

  it('does not render as flagMessage for regular users', () => {
    renderModeBanner({ as: 'wideBanner' }, mockCurrentUser());
    expect(screen.queryByText(/advertisement/)).not.toBeInTheDocument();
  });

  it('does not render if already dismissed', () => {
    renderModeBanner(
      { as: 'wideBanner' },
      mockCurrentUser({
        permissions: { global: ['admin'] },
        dismissedNotices: { [NoticeType.MQR_MODE_ADVERTISEMENT_BANNER]: true },
      }),
    );
    expect(screen.queryByText(/advertisement/)).not.toBeInTheDocument();
  });

  it('does not render if modified', () => {
    modeHandler.setModified();
    renderModeBanner(
      { as: 'wideBanner' },
      mockCurrentUser({
        permissions: { global: ['admin'] },
      }),
    );
    expect(screen.queryByText(/advertisement/)).not.toBeInTheDocument();
  });
});

function renderModeBanner(
  props: Parameters<typeof ModeBanner>[0],
  currentUser = mockCurrentUser(),
) {
  return renderComponent(<ModeBanner {...props} />, '/', { currentUser });
}
