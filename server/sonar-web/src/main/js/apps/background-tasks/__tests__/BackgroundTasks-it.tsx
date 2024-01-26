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
import selectEvent from 'react-select-event';
import ComputeEngineServiceMock from '../../../api/mocks/ComputeEngineServiceMock';
import { parseDate } from '../../../helpers/dates';
import { mockAppState } from '../../../helpers/testMocks';
import { RenderContext, renderAppWithAdminContext } from '../../../helpers/testReactTestingUtils';
import {
  byLabelText,
  byPlaceholderText,
  byRole,
  byTestId,
  byText,
} from '../../../helpers/testSelector';
import { EditionKey } from '../../../types/editions';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import { PAGE_SIZE } from '../constants';
import routes from '../routes';

jest.mock('../constants', () => ({
  ...jest.requireActual('../constants'),
  PAGE_SIZE: 9,
}));

const computeEngineServiceMock = new ComputeEngineServiceMock();

beforeAll(() => {
  computeEngineServiceMock.reset();
});

afterEach(() => computeEngineServiceMock.reset());

describe('The Global background task page', () => {
  it('should display the list of workers and allow edit', async () => {
    const { ui, user } = getPageObject();

    renderGlobalBackgroundTasksApp();
    await ui.appLoaded();

    expect(ui.numberOfWorkers().get()).toHaveTextContent('2');

    const editWorkersButton = screen.getByRole('button', {
      name: 'background_tasks.change_number_of_workers',
    });
    expect(editWorkersButton).toBeInTheDocument();

    await user.click(editWorkersButton);

    const modal = screen.getByRole('dialog');

    expect(
      within(modal).getByRole('heading', { name: 'background_tasks.change_number_of_workers' }),
    ).toBeInTheDocument();

    await user.click(
      within(modal).getByLabelText('background_tasks.change_number_of_workers', {
        selector: 'input',
      }),
    );

    await user.keyboard('[ArrowDown][ArrowDown][Enter]');

    await user.click(within(modal).getByRole('button', { name: 'save' }));

    expect(ui.numberOfWorkers().get()).toHaveTextContent('4');
  });

  it('should display the list of tasks', async () => {
    const { ui, user } = getPageObject();

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
      executedAt: '2022-02-04T11:45:36+0200',
      submittedAt: '2022-02-04T11:45:35+0200',
      componentKey: 'otherComponent',
      status: TaskStatuses.Success,
      type: TaskTypes.AppRefresh,
    });

    renderGlobalBackgroundTasksApp();
    await ui.appLoaded();

    expect(ui.pageHeading.get()).toBeInTheDocument();

    expect(ui.getAllRows()).toHaveLength(4);

    // Status filter.
    await ui.changeTaskFilter('status', 'background_task.status.IN_PROGRESS');
    expect(ui.getAllRows()).toHaveLength(1);

    await ui.changeTaskFilter('status', 'background_task.status.ALL');
    expect(ui.getAllRows()).toHaveLength(5);

    // Type filter.
    await ui.changeTaskFilter('type', `background_task.type.${TaskTypes.AppRefresh}`);
    expect(ui.getAllRows()).toHaveLength(3);

    // Latest analysis.
    await user.click(ui.onlyLatestAnalysis.get());
    expect(ui.getAllRows()).toHaveLength(2);

    // Reset filters.
    await user.click(ui.resetFilters.get());
    expect(ui.getAllRows()).toHaveLength(4);

    // Date filters.
    await ui.setDateRange('2022-02-01', '2022-02-04');
    expect(ui.getAllRows()).toHaveLength(1);

    // Reset filters.
    await user.click(ui.resetFilters.get());
    expect(ui.getAllRows()).toHaveLength(4);

    // Search.
    await user.click(ui.search.get());
    await user.keyboard('other');
    expect(ui.getAllRows()).toHaveLength(1);

    // Reset filters
    await user.click(ui.resetFilters.get());
    expect(ui.getAllRows()).toHaveLength(4);

    // Reset tasks (internally) and reload:
    computeEngineServiceMock.reset();
    await ui.changeTaskFilter('status', 'background_task.status.ALL');
    await user.click(ui.reloadButton.get());
    expect(ui.getAllRows()).toHaveLength(1);
  });

  it.each([[EditionKey.community], [EditionKey.developer], [EditionKey.enterprise]])(
    '%s should not display node name',
    async (editionKey: EditionKey) => {
      const { ui } = getPageObject();
      givenOneTaskWithoutNodeNameAndOneWithNodeName();

      renderGlobalBackgroundTasksApp({ appState: mockAppState({ edition: editionKey }) });

      await waitFor(() => {
        expect(ui.getAllRows()).toHaveLength(2);
      });

      expect(screen.queryByText('background_tasks.table.nodeName')).not.toBeInTheDocument();
      expect(screen.queryByText('best_node_ever')).not.toBeInTheDocument();
    },
  );

  it('DCE edition should display node name', async () => {
    const { ui } = getPageObject();
    givenOneTaskWithoutNodeNameAndOneWithNodeName();

    renderGlobalBackgroundTasksApp({ appState: mockAppState({ edition: EditionKey.datacenter }) });
    await ui.appLoaded();

    expect(screen.getByText('background_tasks.table.nodeName')).toBeInTheDocument();

    expect(within(ui.getAllRows()[0]).getByText('best_node_ever')).toBeInTheDocument();
  });

  it('should handle task pagination', async () => {
    const { ui, user } = getPageObject();

    computeEngineServiceMock.clearTasks();

    const TOTAL_TASKS = 10;

    computeEngineServiceMock.createTasks(TOTAL_TASKS);

    renderGlobalBackgroundTasksApp();
    await ui.appLoaded();

    expect(ui.pageHeading.get()).toBeInTheDocument();

    expect(ui.getAllRows()).toHaveLength(PAGE_SIZE);

    await user.click(ui.showMoreButton.get());

    await waitFor(async () => {
      expect(await screen.findAllByRole('row')).toHaveLength(TOTAL_TASKS + 1); // 1 extra = header
    });
  });

  it('should filter using row list', async () => {
    const { ui, user } = getPageObject();

    computeEngineServiceMock.clearTasks();
    computeEngineServiceMock.addTask({ status: TaskStatuses.Success });
    computeEngineServiceMock.addTask({
      status: TaskStatuses.Failed,
      errorMessage: 'FooError',
      errorStacktrace: 'FooStackTrace',
    });
    computeEngineServiceMock.addTask({
      status: TaskStatuses.Failed,
      warningCount: 2,
      warnings: ['FooWarning1', 'FooWarning2'],
    });
    computeEngineServiceMock.addTask({
      status: TaskStatuses.Success,
      hasScannerContext: true,
      scannerContext: 'FooScannerContext',
    });
    computeEngineServiceMock.addTask({
      componentKey: 'otherComponent',
      status: TaskStatuses.Success,
    });

    renderGlobalBackgroundTasksApp();
    await ui.appLoaded();

    // Filter by first task component
    expect(ui.getAllRows()).toHaveLength(5);
    await ui.clickOnTaskAction(0, 'background_tasks.filter_by_component_x.Foo');
    expect(ui.getAllRows()).toHaveLength(4);

    // Show second task error
    await user.click(ui.resetFilters.get());
    await ui.clickOnTaskAction(1, 'background_tasks.show_stacktrace');
    expect(within(screen.getByRole('dialog')).getByText('FooStackTrace')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'close' }));

    // Show third task warnings
    await user.click(ui.resetFilters.get());
    await ui.clickOnTaskAction(2, 'background_tasks.show_warnings');
    expect(within(screen.getByRole('dialog')).getByText('FooWarning1')).toBeInTheDocument();
    expect(within(screen.getByRole('dialog')).getByText('FooWarning2')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'close' }));

    // Show fourth task scanner context
    await user.click(ui.resetFilters.get());
    await ui.clickOnTaskAction(3, 'background_tasks.show_scanner_context');
    expect(within(screen.getByRole('dialog')).getByText('FooScannerContext')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'close' }));
  });

  it('should cancel all pending tasks', async () => {
    const { ui, user } = getPageObject();

    computeEngineServiceMock.clearTasks();
    computeEngineServiceMock.addTask({
      status: TaskStatuses.Pending,
    });
    computeEngineServiceMock.addTask({
      status: TaskStatuses.Pending,
    });
    computeEngineServiceMock.addTask({
      status: TaskStatuses.Success,
    });

    renderGlobalBackgroundTasksApp({
      appState: mockAppState({ canAdmin: true }),
    });
    await ui.appLoaded();

    // Pending tasks should not be shown by default
    expect(ui.getAllRows()).toHaveLength(1);

    // Verify two pending tasks are shown when filtering by pending
    await ui.changeTaskFilter('status', 'background_task.status.PENDING');
    expect(ui.getAllRows()).toHaveLength(2);

    // Cancel all tasks
    await user.click(ui.cancelAllButton.get());
    await user.click(ui.cancelAllButtonConfirm.get());
    expect(ui.getAllRows()).toHaveLength(0);

    // Go back to default filter, all tasks should now be shown
    await ui.changeTaskFilter('status', 'background_task.status.ALL_EXCEPT_PENDING');
    expect(ui.getAllRows()).toHaveLength(3);
  });
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

function getPageObject() {
  const user = userEvent.setup();

  const selectors = {
    pageHeading: byRole('heading', { name: 'background_tasks.page' }),
    numberOfWorkers: () => byLabelText(`background_tasks.number_of_workers`),
    onlyLatestAnalysis: byRole('switch', {
      name: 'background_tasks.currents_filter.ALL',
    }),
    search: byPlaceholderText('background_tasks.search_by_task_or_component'),
    fromDateInput: byLabelText('start_date'),
    toDateInput: byLabelText('end_date'),
    resetFilters: byRole('button', { name: 'reset_verb' }),
    showMoreButton: byRole('button', { name: 'show_more' }),
    reloadButton: byRole('button', { name: 'reload' }),
    cancelAllButton: byRole('button', { description: 'background_tasks.cancel_all_tasks' }),
    cancelAllButtonConfirm: byText('background_tasks.cancel_all_tasks.submit'),
    row: byRole('row'),
    startDateInput: byPlaceholderText('start_date'),
    monthSelector: byTestId('month-select'),
    yearSelector: byTestId('year-select'),
  };

  const ui = {
    ...selectors,

    async appLoaded() {
      expect(
        await screen.findByRole('heading', { name: 'background_tasks.page' }),
      ).toBeInTheDocument();
    },

    getAllRows() {
      return screen.getAllByRole('row').slice(1); // Excludes header
    },

    async changeTaskFilter(fieldLabel: string, value: string) {
      await selectEvent.select(screen.getByRole('combobox', { name: fieldLabel }), [value]);
      expect(await screen.findByRole('button', { name: 'reload' })).toBeEnabled();
    },

    async setDateRange(from?: string, to?: string) {
      if (from) {
        await this.selectDate(from, ui.fromDateInput.get());
      }

      if (to) {
        await this.selectDate(to, ui.toDateInput.get());
      }
      expect(await screen.findByRole('button', { name: 'reload' })).toBeEnabled();
    },

    async selectDate(date: string, datePickerSelector: HTMLElement) {
      const monthMap = [
        'Jan',
        'Feb',
        'Mar',
        'Apr',
        'May',
        'Jun',
        'Jul',
        'Aug',
        'Sep',
        'Oct',
        'Nov',
        'Dec',
      ];
      const parsedDate = parseDate(date);
      await user.click(datePickerSelector);
      const monthSelector = within(ui.monthSelector.get()).getByRole('combobox');

      await user.click(monthSelector);
      const selectedMonthElements = within(ui.monthSelector.get()).getAllByText(
        monthMap[parseDate(parsedDate).getMonth()],
      );
      await user.click(selectedMonthElements[selectedMonthElements.length - 1]);

      const yearSelector = within(ui.yearSelector.get()).getByRole('combobox');

      await user.click(yearSelector);
      const selectedYearElements = within(ui.yearSelector.get()).getAllByText(
        parseDate(parsedDate).getFullYear(),
      );
      await user.click(selectedYearElements[selectedYearElements.length - 1]);

      await user.click(
        screen.getByText(parseDate(parsedDate).getDate().toString(), { selector: 'button' }),
      );
    },

    async clickOnTaskAction(rowIndex: number, label: string) {
      const row = ui.getAllRows()[rowIndex];
      expect(row).toBeVisible();
      await user.click(within(row).getByRole('button', { name: 'background_tasks.show_actions' }));
      await user.click(within(row).getByRole('menuitem', { name: label }));
    },
  };

  return {
    ui,
    user,
  };
}

function renderGlobalBackgroundTasksApp(context: RenderContext = {}) {
  renderAppWithAdminContext('admin/background_tasks', routes, context);
}
