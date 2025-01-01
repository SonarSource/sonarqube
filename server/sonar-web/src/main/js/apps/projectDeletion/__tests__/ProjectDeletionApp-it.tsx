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
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { deleteApplication } from '../../../api/application';
import { deletePortfolio, deleteProject } from '../../../api/project-management';
import { ComponentContext } from '../../../app/components/componentContext/ComponentContext';
import { mockComponent } from '../../../helpers/mocks/component';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { ComponentContextShape } from '../../../types/component';
import { Component } from '../../../types/types';
import App from '../App';

jest.mock('../../../api/project-management');
jest.mock('../../../api/application');

beforeEach(() => {
  jest.clearAllMocks();
});

it('should be able to delete project', async () => {
  const user = userEvent.setup();

  jest.mock('../../../api/project-management', () => {
    return {
      ...jest.requireActual('../../../api/project-management'),
      deleteProject: jest.fn().mockResolvedValue(undefined),
    };
  });

  renderProjectDeletionApp(
    mockComponent({ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }),
  );

  expect(byText('deletion.page').get()).toBeInTheDocument();
  expect(byText('project_deletion.page.description').get()).toBeInTheDocument();
  await user.click(ui.deleteButton.get());
  expect(await ui.confirmationModal(ComponentQualifier.Project).find()).toBeInTheDocument();

  await user.click(
    ui.confirmationModal(ComponentQualifier.Project).byRole('button', { name: 'delete' }).get(),
  );

  expect(await byText(/project_deletion.resource_dele/).find()).toBeInTheDocument();
  expect(deleteProject).toHaveBeenCalledWith('foo');
});

it('should be able to delete Portfolio', async () => {
  const user = userEvent.setup();

  jest.mock('../../../api/project-management', () => {
    return {
      ...jest.requireActual('../../../api/project-management'),
      deletePortfolio: jest.fn().mockResolvedValue(undefined),
    };
  });

  renderProjectDeletionApp(
    mockComponent({ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Portfolio }),
  );

  expect(byText('deletion.page').get()).toBeInTheDocument();
  expect(byText('portfolio_deletion.page.description').get()).toBeInTheDocument();

  await user.click(ui.deleteButton.get());

  expect(await ui.confirmationModal(ComponentQualifier.Portfolio).find()).toBeInTheDocument();
  await user.click(
    ui.confirmationModal(ComponentQualifier.Portfolio).byRole('button', { name: 'delete' }).get(),
  );

  expect(await byText(/project_deletion.resource_dele/).find()).toBeInTheDocument();
  expect(deletePortfolio).toHaveBeenCalledWith('foo');
});

it('should be able to delete Application', async () => {
  const user = userEvent.setup();

  jest.mock('../../../api/application', () => {
    return {
      ...jest.requireActual('../../../api/application'),
      deleteApplication: jest.fn().mockResolvedValue(undefined),
    };
  });

  renderProjectDeletionApp(
    mockComponent({ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Application }),
  );

  expect(byText('deletion.page').get()).toBeInTheDocument();
  expect(byText('application_deletion.page.description').get()).toBeInTheDocument();

  await user.click(ui.deleteButton.get());
  expect(await ui.confirmationModal(ComponentQualifier.Application).find()).toBeInTheDocument();
  await user.click(
    ui.confirmationModal(ComponentQualifier.Application).byRole('button', { name: 'delete' }).get(),
  );

  expect(await byText(/project_deletion.resource_dele/).find()).toBeInTheDocument();
  expect(deleteApplication).toHaveBeenCalledWith('foo');
});

it('should render with no component', () => {
  renderProjectDeletionApp();

  expect(byText('deletion.page').query()).not.toBeInTheDocument();
});

function renderProjectDeletionApp(component?: Component) {
  renderApp(
    'project-delete',
    <ComponentContext.Provider value={{ component } as ComponentContextShape}>
      <App />
    </ComponentContext.Provider>,
  );
}

const ui = {
  confirmationModal: (qualifier: ComponentQualifier) =>
    byRole('alertdialog', { name: `qualifier.delete.${qualifier}` }),
  deleteButton: byRole('button', { name: 'delete' }),
};
