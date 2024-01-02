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
import { createProject, doesComponentExists } from '../../../../api/components';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import ManualProjectCreate from '../ManualProjectCreate';

jest.mock('../../../../api/components', () => ({
  createProject: jest.fn().mockResolvedValue({ project: { key: 'bar', name: 'Bar' } }),
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
    await screen.findByText('onboarding.create_project.pr_decoration.information')
  ).toBeInTheDocument();
});

it('should validate form input', async () => {
  const user = userEvent.setup();
  renderManualProjectCreate();

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    })
  );
  await user.keyboard('test');
  expect(
    screen.getByRole('textbox', { name: 'onboarding.create_project.project_key field_required' })
  ).toHaveValue('test');
  expect(screen.getByRole('button', { name: 'set_up' })).toBeEnabled();

  // Sanitize the key
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    })
  );
  await user.keyboard('{Control>}a{/Control}This is not a key%^$');
  expect(
    screen.getByRole('textbox', { name: 'onboarding.create_project.project_key field_required' })
  ).toHaveValue('This-is-not-a-key-');

  // Clear name
  await user.clear(
    screen.getByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    })
  );
  expect(
    screen.getByRole('textbox', { name: 'onboarding.create_project.project_key field_required' })
  ).toHaveValue('');
  expect(
    screen.getByText('onboarding.create_project.display_name.error.empty')
  ).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'set_up' })).toBeDisabled();

  // Only key
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.project_key field_required',
    })
  );
  await user.keyboard('awsome-key');
  expect(
    screen.getByRole('textbox', { name: 'onboarding.create_project.display_name field_required' })
  ).toHaveValue('');
  expect(screen.getByLabelText('valid_input')).toBeInTheDocument();
  expect(
    screen.getByText('onboarding.create_project.display_name.error.empty')
  ).toBeInTheDocument();

  // Invalid key
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.project_key field_required',
    })
  );
  await user.keyboard('{Control>}a{/Control}123');
  expect(
    await screen.findByText('onboarding.create_project.project_key.error.only_digits')
  ).toBeInTheDocument();
  await user.keyboard('{Control>}a{/Control}@');
  expect(
    await screen.findByText('onboarding.create_project.project_key.error.invalid_char')
  ).toBeInTheDocument();
  await user.keyboard('{Control>}a{/Control}exists');
  expect(
    await screen.findByText('onboarding.create_project.project_key.taken')
  ).toBeInTheDocument();

  // Invalid main branch name
  await user.clear(
    screen.getByRole('textbox', {
      name: 'onboarding.create_project.main_branch_name field_required',
    })
  );
  expect(
    await screen.findByText('onboarding.create_project.main_branch_name.error.empty')
  ).toBeInTheDocument();
});

it('should submit form input', async () => {
  const user = userEvent.setup();
  const onProjectCreate = jest.fn();
  renderManualProjectCreate({ onProjectCreate });

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    })
  );
  await user.keyboard('test');
  await user.click(screen.getByRole('button', { name: 'set_up' }));
  expect(createProject).toHaveBeenCalledWith({
    name: 'test',
    project: 'test',
    mainBranch: 'main',
  });
  expect(onProjectCreate).toHaveBeenCalled();
});

it('should handle create failure', async () => {
  const user = userEvent.setup();
  (createProject as jest.Mock).mockRejectedValueOnce({});
  const onProjectCreate = jest.fn();
  renderManualProjectCreate({ onProjectCreate });

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    })
  );
  await user.keyboard('test');
  await user.click(screen.getByRole('button', { name: 'set_up' }));

  expect(onProjectCreate).not.toHaveBeenCalled();
});

it('should handle component exists failure', async () => {
  const user = userEvent.setup();
  (doesComponentExists as jest.Mock).mockRejectedValueOnce({});
  const onProjectCreate = jest.fn();
  renderManualProjectCreate({ onProjectCreate });

  // All input valid
  await user.click(
    await screen.findByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    })
  );
  await user.keyboard('test');
  expect(
    screen.getByRole('textbox', { name: 'onboarding.create_project.display_name field_required' })
  ).toHaveValue('test');
});

function renderManualProjectCreate(props: Partial<ManualProjectCreate['props']> = {}) {
  renderComponent(
    <ManualProjectCreate branchesEnabled={false} onProjectCreate={jest.fn()} {...props} />
  );
}
