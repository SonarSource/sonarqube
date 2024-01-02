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
import React from 'react';
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import SystemUpgradeButton from '../SystemUpgradeButton';
import { UpdateUseCase } from '../utils';

const ui = {
  learnMoreButton: byRole('button', { name: 'learn_more' }),

  header: byRole('heading', { name: 'system.system_upgrade' }),
  downloadLink: byRole('link', { name: /system.see_sonarqube_downloads/ }),

  ltsVersionHeader: byRole('heading', { name: /system.lts_version/ }),

  newPatchWarning: byText(/admin_notification.update/),
};

it('should render properly', async () => {
  const user = userEvent.setup();

  renderSystemUpgradeButton();

  await user.click(ui.learnMoreButton.get());

  expect(ui.header.get()).toBeInTheDocument();
  expect(ui.ltsVersionHeader.get()).toBeInTheDocument();
  expect(ui.downloadLink.get()).toBeInTheDocument();
});

it('should render properly for new patch', async () => {
  const user = userEvent.setup();

  renderSystemUpgradeButton(
    {
      updateUseCase: UpdateUseCase.NewPatch,
      latestLTS: '9.9',
      systemUpgrades: [{ downloadUrl: '', version: '9.9.1' }],
    },
    '9.9',
  );

  await user.click(ui.learnMoreButton.get());

  expect(ui.header.get()).toBeInTheDocument();
  expect(ui.newPatchWarning.get()).toBeInTheDocument();
  expect(ui.ltsVersionHeader.get()).toBeInTheDocument();
  expect(ui.downloadLink.get()).toBeInTheDocument();
});

function renderSystemUpgradeButton(
  props: Partial<SystemUpgradeButton['props']> = {},
  version = '9.7',
) {
  renderComponent(
    <SystemUpgradeButton
      latestLTS="9.9"
      systemUpgrades={[
        { downloadUrl: 'eight', version: '9.8' },
        { downloadUrl: 'lts', version: '9.9' },
        { downloadUrl: 'patch', version: '9.9.1' },
      ]}
      {...props}
    />,
    '',
    { appState: mockAppState({ version }) },
  );
}
