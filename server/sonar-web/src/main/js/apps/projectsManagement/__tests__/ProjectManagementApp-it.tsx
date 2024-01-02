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
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getComponents, SearchProjectsParameters } from '../../../api/components';
import PermissionTemplateServiceMock from '../../../api/mocks/PermissionTemplateServiceMock';
import { renderAppWithAdminContext } from '../../../helpers/testReactTestingUtils';
import { ComponentQualifier, Visibility } from '../../../types/component';
import routes from '../routes';

jest.mock('../../../api/components', () => ({
  getComponents: jest.fn().mockResolvedValue({
    paging: { total: 0 },
    components: [],
  }),
}));

jest.mock('../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({ value: 'public' }),
}));

const components = mockComponents(11);

let permissionTemplateMock: PermissionTemplateServiceMock;
beforeAll(() => {
  permissionTemplateMock = new PermissionTemplateServiceMock();
});

afterEach(() => {
  permissionTemplateMock.reset();
});

describe('Bulk Apply', () => {
  (getComponents as jest.Mock).mockImplementation(getComponentsImplementation(10));

  it('should work as expected', async () => {
    const user = userEvent.setup();

    renderGlobalBackgroundTasksApp();

    const bulkApplyButton = await screen.findByRole('button', {
      name: 'permission_templates.bulk_apply_permission_template',
    });

    expect(bulkApplyButton).toBeDisabled();

    const projects = getProjects();

    expect(projects).toHaveLength(10);

    await user.click(screen.getByRole('button', { name: 'show_more' }));

    expect(getProjects()).toHaveLength(11);

    await user.click(screen.getByRole('checkbox', { name: 'check_all' }));

    expect(bulkApplyButton).toBeEnabled();

    await user.click(bulkApplyButton);

    let modal = await screen.findByRole('dialog');

    expect(modal).toBeInTheDocument();
    expect(
      within(modal).getByText(
        'permission_templates.bulk_apply_permission_template.apply_to_selected.11'
      )
    ).toBeInTheDocument();

    await user.click(within(modal).getByRole('button', { name: 'apply' }));

    expect(
      await screen.findByText('bulk apply permission template error message')
    ).toBeInTheDocument();
    expect(await screen.findByRole('dialog')).toBeInTheDocument();

    await user.click(within(modal).getByRole('button', { name: 'cancel' }));

    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[8]);
    await user.click(checkboxes[9]);

    await user.click(bulkApplyButton);

    modal = await screen.findByRole('dialog');

    expect(modal).toBeInTheDocument();
    expect(
      within(modal).getByText(
        'permission_templates.bulk_apply_permission_template.apply_to_selected.9'
      )
    ).toBeInTheDocument();

    await user.click(within(modal).getByRole('button', { name: 'apply' }));

    modal = await screen.findByRole('dialog');
    expect(
      await within(modal).findByText('projects_role.apply_template.success')
    ).toBeInTheDocument();
  });
});

function getProjects() {
  // remove the first 2 rows: first is the filter bar, and second is the header
  return screen.getAllByRole('row').slice(2);
}

function getComponentsImplementation(overridePageSize?: number) {
  return (params: SearchProjectsParameters) => {
    const pageSize = overridePageSize ?? params.ps ?? 50;
    const startIndex = ((params.p ?? 1) - 1) * pageSize; // artifically bump the page size to 500
    return Promise.resolve({
      paging: {
        total: 1001,
      },
      components: components.slice(startIndex, startIndex + pageSize),
    });
  };
}

function mockComponents(n: number) {
  const results = [];

  for (let i = 0; i < n; i++) {
    results.push({
      key: `project-${i + 1}`,
      name: `Project ${i + 1}`,
      qualifier: ComponentQualifier.Project,
      visibility: Visibility.Private,
    });
  }

  return results;
}

function renderGlobalBackgroundTasksApp() {
  renderAppWithAdminContext('admin/projects_management', routes, {});
}
