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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { createProject } from '../../../api/components';
import CreateProjectForm from '../CreateProjectForm';

jest.mock('../../../api/components', () => ({
  createProject: jest.fn().mockResolvedValue({}),
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists')),
}));

jest.mock('../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({ value: 'main' }),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render all inputs and create a project', async () => {
  const user = userEvent.setup();
  renderCreateProjectForm();

  await user.type(
    screen.getByRole('textbox', {
      name: 'onboarding.create_project.display_name field_required',
    }),
    'ProjectName'
  );

  await user.type(
    screen.getByRole('textbox', {
      name: 'onboarding.create_project.project_key field_required',
    }),
    'ProjectKey'
  );

  expect(
    screen.getByRole('textbox', {
      name: 'onboarding.create_project.main_branch_name field_required',
    })
  ).toHaveValue('main');

  await user.type(
    screen.getByRole('textbox', {
      name: 'onboarding.create_project.main_branch_name field_required',
    }),
    '{Control>}a{/Control}{Backspace}ProjectMainBranch'
  );

  await user.click(screen.getByRole('button', { name: 'create' }));
  expect(createProject).toHaveBeenCalledWith({
    name: 'ProjectName',
    project: 'ProjectKey',
    mainBranch: 'ProjectMainBranch',
  });
});

function renderCreateProjectForm(props: Partial<CreateProjectForm['props']> = {}) {
  render(<CreateProjectForm onClose={jest.fn()} onProjectCreated={jest.fn()} {...props} />);
}
