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
import * as React from 'react';
import { doesComponentExists } from '../../../../api/components';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../helpers/testSelector';
import ManualProjectCreate from '../manual/ManualProjectCreate';

const ui = {
  nextButton: byRole('button', { name: 'next' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  closeButton: byRole('button', { name: 'clear' }),
};

jest.mock('../../../../api/components', () => ({
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists')),
}));

jest.mock('../../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({ value: 'main' }),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should show branch information', async () => {
  renderManualProjectCreate({ branchesEnabled: true });
  expect(
    await screen.findByText('onboarding.create_project.pr_decoration.information'),
  ).toBeInTheDocument();
});

it('should validate form input', async () => {
  const user = userEvent.setup();
  renderManualProjectCreate();

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: /onboarding.create_project.display_name/,
    }),
  );
  await user.keyboard('test');
  expect(
    screen.getByRole('textbox', { name: /onboarding.create_project.project_key/ }),
  ).toHaveValue('test');
  expect(ui.nextButton.get()).toBeEnabled();

  // Sanitize the key
  await user.click(
    await screen.findByRole('textbox', {
      name: /onboarding.create_project.display_name/,
    }),
  );
  await user.keyboard('{Control>}a{/Control}This is not a key%^$');
  expect(
    screen.getByRole('textbox', { name: /onboarding.create_project.project_key/ }),
  ).toHaveValue('This-is-not-a-key-');

  // Clear name
  await user.clear(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.display_name/,
    }),
  );
  expect(
    screen.getByRole('textbox', { name: /onboarding.create_project.project_key/ }),
  ).toHaveValue('');

  expect(ui.nextButton.get()).toBeDisabled();

  // Only key
  await user.click(
    await screen.findByRole('textbox', {
      name: /onboarding.create_project.project_key/,
    }),
  );
  await user.keyboard('awsome-key');
  expect(
    screen.getByRole('textbox', { name: /onboarding.create_project.display_name/ }),
  ).toHaveValue('');
  expect(ui.nextButton.get()).toBeDisabled();

  // Invalid key
  await user.click(
    await screen.findByRole('textbox', {
      name: /onboarding.create_project.project_key/,
    }),
  );
  await user.keyboard('{Control>}a{/Control}123');
  expect(ui.nextButton.get()).toBeDisabled();

  await user.keyboard('{Control>}a{/Control}@');
  expect(ui.nextButton.get()).toBeDisabled();

  await user.keyboard('{Control>}a{/Control}exists');
  expect(ui.nextButton.get()).toBeDisabled();

  // Invalid main branch name
  await user.clear(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.main_branch_name/,
    }),
  );
  expect(ui.nextButton.get()).toBeDisabled();
});

it('should submit form input', async () => {
  const user = userEvent.setup();
  const onProjectSetupDone = jest.fn();
  renderManualProjectCreate({ onProjectSetupDone });

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: /onboarding.create_project.display_name/,
    }),
  );
  await user.keyboard('test');
  await user.click(ui.nextButton.get());
  expect(onProjectSetupDone).toHaveBeenCalled();
});

it('should handle component exists failure', async () => {
  const user = userEvent.setup();
  jest.mocked(doesComponentExists).mockRejectedValueOnce({});
  renderManualProjectCreate();

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: /onboarding.create_project.display_name/,
    }),
  );
  await user.keyboard('test');
  expect(
    screen.getByRole('textbox', { name: /onboarding.create_project.display_name/ }),
  ).toHaveValue('test');
});

function renderManualProjectCreate(props: Partial<Parameters<typeof ManualProjectCreate>[0]> = {}) {
  renderComponent(
    <ManualProjectCreate
      branchesEnabled={false}
      onProjectSetupDone={jest.fn()}
      onClose={jest.fn()}
      {...props}
    />,
  );
}
