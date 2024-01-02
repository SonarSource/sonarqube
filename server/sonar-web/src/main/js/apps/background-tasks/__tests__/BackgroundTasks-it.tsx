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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import ComputeEngineServiceMock from '../../../api/mocks/ComputeEngineServiceMock';
import { mockAppState } from '../../../helpers/testMocks';
import { renderAppWithAdminContext, RenderContext } from '../../../helpers/testReactTestingUtils';
import { EditionKey } from '../../../types/editions';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import routes from '../routes';

jest.mock('../../../api/ce');

jest.mock('../constants', () => {
  const contants = jest.requireActual('../constants');

  return { ...contants, PAGE_SIZE: 9 };
});

let computeEngineServiceMock: ComputeEngineServiceMock;

beforeAll(() => {
  computeEngineServiceMock = new ComputeEngineServiceMock();
});

afterEach(() => computeEngineServiceMock.reset());

describe('The Global background task page', () => {
  it('should display the list of workers and allow edit', async () => {
    const user = userEvent.setup();

    renderGlobalBackgroundTasksApp();

    expect(
      within(await screen.findByText('background_tasks.number_of_workers')).getByText('2')
    ).toBeInTheDocument();

    const editWorkersButton = screen.getByRole('button', {
      name: 'background_tasks.change_number_of_workers',
    });
    expect(editWorkersButton).toBeInTheDocument();

    await user.click(editWorkersButton);

    const modal = screen.getByRole('dialog');

    expect(
      within(modal).getByRole('heading', { name: 'background_tasks.change_number_of_workers' })
    ).toBeInTheDocument();

    await user.click(
      within(modal).getByLabelText('background_tasks.change_number_of_workers', {
        selector: 'input',
      })
    );

    await user.keyboard('[ArrowDown][ArrowDown][Enter]');

    await user.click(within(modal).getByRole('button', { name: 'save' }));

    expect(
      within(await screen.findByText('background_tasks.number_of_workers')).getByText('4')
    ).toBeInTheDocument();
  });

  it('should display the list of tasks', async () => {
    const user = userEvent.setup();

    computeEngineServiceMock.clearTasks();
    computeEngineServiceMock.addTask({ status: TaskStatuses.Canceled, type: TaskTypes.AppRefresh });
    computeEngineServiceMock.addTask({ status: TaskStatuses.Failed, type: TaskTypes.AppRefresh });
    computeEngineServiceMock.addTask({
      executedAt: '2022-02-03T11:45:36+0200',
      submittedAt: '2022-02-03T11:45:35+0200',
      executionTimeMs: 167,
      status: TaskStatuses.InProgress,
      type: TaskTypes.IssueSync,
    });
    computeEngineServiceMock.addTask({ status: TaskStatuses.Pending, type: TaskTypes.IssueSync });
    computeEngineServiceMock.addTask({
      componentKey: 'otherComponent',
      status: TaskStatuses.Success,
      type: TaskTypes.AppRefresh,
    });

    renderGlobalBackgroundTasksApp();

    expect(
      await screen.findByRole('heading', { name: 'background_tasks.page' })
    ).toBeInTheDocument();

    expect(screen.getAllByRole('row')).toHaveLength(5); // including header, excluding pending (default filter)

    await changeTaskFilter(user, 'status', 'background_task.status.IN_PROGRESS');
    expect(await screen.findAllByRole('row')).toHaveLength(2); // including header

    await changeTaskFilter(user, 'status', 'background_task.status.ALL');
    expect(await screen.findAllByRole('row')).toHaveLength(6); // including header

    await changeTaskFilter(user, 'type', `background_task.type.${TaskTypes.AppRefresh}`);
    expect(await screen.findAllByRole('row')).toHaveLength(4); // including header

    await user.click(screen.getByRole('checkbox', { name: 'yes' }));
    expect(await screen.findAllByRole('row')).toHaveLength(3); // including header
    await user.click(screen.getByRole('checkbox', { name: 'yes' }));

    /*
     * Must test date range filters, but it requires refactoring the DateRange component
     */

    const searchBox = screen.getByPlaceholderText('background_tasks.search_by_task_or_component');
    expect(searchBox).toBeInTheDocument();
    await user.click(searchBox);
    await user.keyboard('other');

    expect(await screen.findAllByRole('row')).toHaveLength(2); // including header

    //reset filters
    await user.click(screen.getByRole('button', { name: 'reset_verb' }));
    expect(screen.getAllByRole('row')).toHaveLength(5);

    // reset tasks (internally) and reload:
    computeEngineServiceMock.reset();
    await changeTaskFilter(user, 'status', 'background_task.status.ALL');
    await user.click(screen.getByRole('button', { name: 'reload' }));
    expect(await screen.findAllByRole('row')).toHaveLength(2);
  });

  it.each([[EditionKey.community], [EditionKey.developer], [EditionKey.enterprise]])(
    'Editions %s should not display node name',
    async (editionKey: EditionKey) => {
      givenOneTaskWithoutNodeNameAndOneWithNodeName();

      renderGlobalBackgroundTasksApp({ appState: mockAppState({ edition: editionKey }) });

      await waitFor(() => {
        expect(screen.getAllByRole('row')).toHaveLength(3); // including header
      });

      expect(screen.queryByText('background_tasks.table.nodeName')).not.toBeInTheDocument();
      expect(screen.queryByText('best_node_ever')).not.toBeInTheDocument();
    }
  );

  it('Node name should be shown in DCE edition', async () => {
    givenOneTaskWithoutNodeNameAndOneWithNodeName();

    renderGlobalBackgroundTasksApp({ appState: mockAppState({ edition: EditionKey.datacenter }) });

    await waitFor(async () => {
      expect(await screen.findByText('background_tasks.table.nodeName')).toBeInTheDocument();
    });

    expect(
      within(await screen.getAllByRole('row')[1]).getByText('best_node_ever')
    ).toBeInTheDocument();
  });

  it('should handle task pagination', async () => {
    const user = userEvent.setup();

    computeEngineServiceMock.clearTasks();
    computeEngineServiceMock.createTasks(10);

    renderGlobalBackgroundTasksApp();

    expect(
      await screen.findByRole('heading', { name: 'background_tasks.page' })
    ).toBeInTheDocument();

    expect(screen.getAllByRole('row')).toHaveLength(10); // including header

    user.click(screen.getByRole('button', { name: 'show_more' }));

    await waitFor(() => {
      expect(screen.getAllByRole('row')).toHaveLength(11); // including header
    });
  });

  /*
   * Must also test row actions
   */
});

function givenOneTaskWithoutNodeNameAndOneWithNodeName() {
  computeEngineServiceMock.clearTasks();
  computeEngineServiceMock.addTask({
    executedAt: '2022-02-03T11:45:36+0200',
    submittedAt: '2022-02-03T11:45:35+0200',
    executionTimeMs: 167,
    status: TaskStatuses.InProgress,
    type: TaskTypes.IssueSync,
    nodeName: 'best_node_ever',
  });
  computeEngineServiceMock.addTask({
    executedAt: '2022-02-03T11:45:35+0200',
    submittedAt: '2022-02-03T11:45:34+0200',
    executionTimeMs: 167,
    status: TaskStatuses.InProgress,
    type: TaskTypes.IssueSync,
  });
}

async function changeTaskFilter(user: UserEvent, fieldLabel: string, value: string) {
  await user.click(screen.getByLabelText(fieldLabel, { selector: 'input' }));
  await user.click(screen.getByText(value));
}

function renderGlobalBackgroundTasksApp(context: RenderContext = {}) {
  renderAppWithAdminContext('admin/background_tasks', routes, context);
}
