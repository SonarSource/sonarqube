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

import userEvent from '@testing-library/user-event';
import { ModeServiceMock } from '../../../api/mocks/ModeServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { mockAppState, mockCurrentUser, mockLocation } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { byRole } from '../../../sonar-aligned/helpers/testSelector';
import { Permissions } from '../../../types/permissions';
import { NoticeType } from '../../../types/users';
import ModeTour from '../ModeTour';
import GlobalNav from '../nav/global/GlobalNav';

const ui = {
  dialog: byRole('dialog'),
  step1Dialog: byRole('dialog', { name: /mode_tour.step1.title/ }),
  step2Dialog: byRole('dialog', { name: /mode_tour.step2.title/ }),
  step3Dialog: byRole('dialog', { name: /mode_tour.step3.title/ }),
  next: byRole('button', { name: 'next' }),
  later: byRole('button', { name: 'later' }),
  skip: byRole('button', { name: 'skip' }),
  letsgo: byRole('button', { name: 'lets_go' }),
  help: byRole('button', { name: 'help' }),
  guidePopup: byRole('alertdialog'),
  tourTrigger: byRole('menuitem', { name: 'mode_tour.name' }),
};

const settingsHandler = new SettingsServiceMock();
const modeHandler = new ModeServiceMock();
const usersHandler = new UsersServiceMock();

beforeEach(() => {
  settingsHandler.reset();
  modeHandler.reset();
  usersHandler.reset();
});

it('renders the tour for admin', async () => {
  const user = userEvent.setup();
  renderGlobalNav(mockCurrentUser({ permissions: { global: [Permissions.Admin] } }));
  expect(ui.step1Dialog.get()).toBeInTheDocument();
  expect(ui.later.get()).toBeInTheDocument();
  expect(ui.next.query()).not.toBeInTheDocument();
  expect(ui.letsgo.get()).toBeInTheDocument();
  expect(ui.step1Dialog.get()).toHaveTextContent('guiding.step_x_of_y.1.4');
  await user.click(ui.letsgo.get());

  expect(ui.step2Dialog.get()).toBeInTheDocument();
  expect(ui.step1Dialog.query()).not.toBeInTheDocument();
  expect(ui.later.query()).not.toBeInTheDocument();
  expect(ui.next.get()).toBeInTheDocument();
  expect(ui.letsgo.query()).not.toBeInTheDocument();
  expect(ui.step2Dialog.get()).toHaveTextContent('guiding.step_x_of_y.2.4');
  await user.click(ui.next.get());

  expect(ui.step3Dialog.get()).toBeInTheDocument();
  expect(ui.step2Dialog.query()).not.toBeInTheDocument();
  expect(ui.next.get()).toBeInTheDocument();
  expect(ui.step3Dialog.get()).toHaveTextContent('guiding.step_x_of_y.3.4');
  await user.click(ui.next.get());

  expect(ui.dialog.query()).not.toBeInTheDocument();
  expect(await ui.guidePopup.find()).toBeInTheDocument();
  expect(ui.guidePopup.get()).toHaveTextContent('guiding.step_x_of_y.4.4');
  expect(ui.guidePopup.by(ui.next).get()).toBeInTheDocument();
  expect(ui.tourTrigger.query()).not.toBeInTheDocument();
  await user.click(ui.next.get());

  expect(ui.tourTrigger.get()).toBeInTheDocument();
  expect(await ui.guidePopup.find()).toBeInTheDocument();
  expect(ui.guidePopup.query()).not.toHaveTextContent('guiding.step_x_of_y');
  expect(ui.next.query()).not.toBeInTheDocument();
  expect(ui.skip.get()).toBeInTheDocument();
  await user.click(ui.skip.get());

  expect(ui.tourTrigger.query()).not.toBeInTheDocument();
  expect(ui.dialog.query()).not.toBeInTheDocument();

  // replay the tour
  await user.click(ui.help.get());
  await user.click(ui.tourTrigger.get());
  expect(ui.step1Dialog.get()).toBeInTheDocument();
  expect(ui.step1Dialog.get()).toHaveTextContent('guiding.step_x_of_y.1.4');
});

it('renders the tour for gateadmins', async () => {
  const user = userEvent.setup();
  renderGlobalNav(mockCurrentUser({ permissions: { global: [Permissions.QualityGateAdmin] } }));
  expect(ui.step1Dialog.get()).toBeInTheDocument();
  expect(ui.later.get()).toBeInTheDocument();
  expect(ui.next.query()).not.toBeInTheDocument();
  expect(ui.letsgo.get()).toBeInTheDocument();
  expect(ui.step1Dialog.get()).toHaveTextContent('guiding.step_x_of_y.1.3');
  await user.click(ui.letsgo.get());

  expect(ui.step2Dialog.get()).toBeInTheDocument();
  expect(ui.step1Dialog.query()).not.toBeInTheDocument();
  expect(ui.later.query()).not.toBeInTheDocument();
  expect(ui.next.get()).toBeInTheDocument();
  expect(ui.letsgo.query()).not.toBeInTheDocument();
  expect(ui.step2Dialog.get()).toHaveTextContent('guiding.step_x_of_y.2.3');
  await user.click(ui.next.get());

  expect(ui.step3Dialog.get()).toBeInTheDocument();
  expect(ui.step2Dialog.query()).not.toBeInTheDocument();
  expect(ui.next.get()).toBeInTheDocument();
  expect(ui.step3Dialog.get()).toHaveTextContent('guiding.step_x_of_y.3.3');
  await user.click(ui.next.get());

  expect(ui.dialog.query()).not.toBeInTheDocument();
  expect(ui.tourTrigger.get()).toBeInTheDocument();
  expect(await ui.guidePopup.find()).toBeInTheDocument();
  expect(ui.guidePopup.query()).not.toHaveTextContent('guiding.step_x_of_y');
  expect(ui.next.query()).not.toBeInTheDocument();
  expect(ui.skip.get()).toBeInTheDocument();
  await user.click(ui.skip.get());

  expect(ui.tourTrigger.query()).not.toBeInTheDocument();
  expect(ui.dialog.query()).not.toBeInTheDocument();

  // replay the tour
  await user.click(ui.help.get());
  await user.click(ui.tourTrigger.get());
  expect(ui.step1Dialog.get()).toBeInTheDocument();
  expect(ui.step1Dialog.get()).toHaveTextContent('guiding.step_x_of_y.1.3');
});

it('should not render the tour for regular users', async () => {
  const user = userEvent.setup();
  renderGlobalNav(mockCurrentUser({ permissions: { global: [] } }));
  expect(ui.dialog.query()).not.toBeInTheDocument();
  await user.click(ui.help.get());
  expect(ui.tourTrigger.query()).not.toBeInTheDocument();
});

it('should not render the tour if it is already dismissed', async () => {
  const user = userEvent.setup();
  renderGlobalNav(
    mockCurrentUser({
      permissions: { global: [Permissions.Admin] },
      dismissedNotices: { [NoticeType.MODE_TOUR]: true },
    }),
  );
  expect(ui.dialog.query()).not.toBeInTheDocument();
  await user.click(ui.help.get());
  expect(ui.tourTrigger.get()).toBeInTheDocument();

  await user.click(ui.tourTrigger.get());
  expect(ui.step1Dialog.get()).toBeInTheDocument();
  expect(ui.step1Dialog.get()).toHaveTextContent('guiding.step_x_of_y.1.4');
});

function renderGlobalNav(currentUser = mockCurrentUser()) {
  renderApp(
    '/',
    <>
      <GlobalNav location={mockLocation()} />
      <ModeTour />
    </>,
    {
      currentUser,
      appState: mockAppState({ canAdmin: currentUser.permissions?.global.includes('admin') }),
    },
  );
}
