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
import ComputeEngineServiceMock from '../../../api/mocks/ComputeEngineServiceMock';
import { ProjectDumpServiceMock } from '../../../api/mocks/ProjectDumpServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { Feature } from '../../../types/features';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import routes from '../routes';

const computeEngineHandler = new ComputeEngineServiceMock();
const handler = new ProjectDumpServiceMock(computeEngineHandler);

const COMPONENT_KEY = 'test';

const ui = {
  pageDescriptionWithImport: byText('project_dump.page.description1'),
  pageDescriptionWithoutImport: byText('project_dump.page.description_without_import1'),

  disabledImportFeatureMsg: byText('project_dump.import_form_description_disabled'),

  exportBtn: byRole('button', { name: 'project_dump.do_export' }),
  importBtn: byRole('button', { name: 'project_dump.do_import' }),

  successExport: byText('project_dump.latest_export_available.June 8, 2023', { exact: false }),
  pendingExport: byText('project_dump.pending_export.June 8, 2023', { exact: false }),
  inProgressExport: byText('project_dump.in_progress_export.1 hour ago'),
  failedExport: byText('project_dump.failed_export'),
  cantExportMsg: byText('project_dump.can_not_export'),

  successImport: byText('project_dump.import_success.June 8, 2023', { exact: false }),
  pendingImport: byText('project_dump.pending_import.June 8, 2023', { exact: false }),
  inProgressImport: byText('project_dump.in_progress_import.1 hour ago'),
  failedImport: byText('project_dump.failed_import'),
  cantImportMsg: byText('project_dump.can_not_import'),
  noDumpImportMsg: byText('project_dump.no_file_to_import'),
};

beforeEach(() => {
  computeEngineHandler.reset();
  handler.reset();
  jest.useFakeTimers({
    advanceTimers: true,
    now: new Date('2023-06-08T13:00:00Z'),
  });
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('can export project, but can not import', async () => {
  renderProjectKeyApp([Feature.ProjectImport]);
  expect(await ui.exportBtn.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();
  expect(ui.pageDescriptionWithImport.get()).toBeInTheDocument();
  expect(ui.pageDescriptionWithoutImport.query()).not.toBeInTheDocument();
  expect(ui.cantImportMsg.get()).toBeInTheDocument();
  await userEvent.click(ui.exportBtn.get());
  expect(await ui.successExport.find()).toBeInTheDocument();
  expect(screen.getByText(`/tmp/${COMPONENT_KEY}.zip`)).toBeInTheDocument();
});

it('can export project without import feature', async () => {
  renderProjectKeyApp([]);
  expect(await ui.exportBtn.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();
  expect(ui.pageDescriptionWithoutImport.get()).toBeInTheDocument();
  expect(ui.pageDescriptionWithImport.query()).not.toBeInTheDocument();
  expect(ui.disabledImportFeatureMsg.get()).toBeInTheDocument();
  await userEvent.click(ui.exportBtn.get());
  expect(await ui.successExport.find()).toBeInTheDocument();
  expect(screen.getByText(`/tmp/${COMPONENT_KEY}.zip`)).toBeInTheDocument();
});

it('should show pending->in progress->failed export', async () => {
  handler.useCustomTasks();
  renderProjectKeyApp([]);
  computeEngineHandler.addTask({
    componentKey: COMPONENT_KEY,
    type: TaskTypes.ProjectExport,
    status: TaskStatuses.Pending,
    submittedAt: '2023-06-08T11:55:00Z',
  });
  await userEvent.click(await ui.exportBtn.find());
  expect(await ui.pendingExport.find()).toBeInTheDocument();
  expect(ui.exportBtn.query()).not.toBeInTheDocument();

  computeEngineHandler.addTask({
    componentKey: COMPONENT_KEY,
    type: TaskTypes.ProjectExport,
    status: TaskStatuses.InProgress,
    startedAt: '2023-06-08T12:00:00Z',
  });
  jest.runOnlyPendingTimers();
  expect(await ui.inProgressExport.find()).toBeInTheDocument();
  expect(ui.exportBtn.query()).not.toBeInTheDocument();

  computeEngineHandler.addTask({
    componentKey: COMPONENT_KEY,
    type: TaskTypes.ProjectExport,
    status: TaskStatuses.Failed,
    executedAt: '2023-06-08T12:05:00Z',
  });
  jest.runOnlyPendingTimers();
  expect(await ui.failedExport.find()).toBeInTheDocument();
  expect(ui.exportBtn.get()).toBeInTheDocument();
});

it('can import project once, and can not export', async () => {
  handler.setImportState();
  renderProjectKeyApp([Feature.ProjectImport]);
  expect(await ui.importBtn.find()).toBeInTheDocument();
  expect(ui.exportBtn.query()).not.toBeInTheDocument();
  expect(ui.cantExportMsg.get()).toBeInTheDocument();
  await userEvent.click(ui.importBtn.get());
  expect(await ui.successImport.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();
});

it('should show pending->in progress->failed import', async () => {
  handler.useCustomTasks();
  handler.setImportState();
  renderProjectKeyApp([Feature.ProjectImport]);
  computeEngineHandler.addTask({
    componentKey: COMPONENT_KEY,
    type: TaskTypes.ProjectImport,
    status: TaskStatuses.Pending,
    submittedAt: '2023-06-08T11:55:00Z',
  });
  await userEvent.click(await ui.importBtn.find());
  expect(await ui.pendingImport.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();

  computeEngineHandler.addTask({
    componentKey: COMPONENT_KEY,
    type: TaskTypes.ProjectImport,
    status: TaskStatuses.InProgress,
    startedAt: '2023-06-08T12:00:00Z',
  });
  jest.runOnlyPendingTimers();
  expect(await ui.inProgressImport.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();

  computeEngineHandler.addTask({
    componentKey: COMPONENT_KEY,
    type: TaskTypes.ProjectImport,
    status: TaskStatuses.Failed,
    executedAt: '2023-06-08T12:05:00Z',
  });
  jest.runOnlyPendingTimers();
  expect(await ui.failedImport.find()).toBeInTheDocument();
  expect(ui.importBtn.get()).toBeInTheDocument();
});

it(`can't import if no dump file`, async () => {
  handler.setImportState();
  handler.dumpToImport = undefined;
  renderProjectKeyApp([Feature.ProjectImport]);
  expect(await ui.noDumpImportMsg.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();
});

it('can do nothing', async () => {
  handler.setImportState();
  renderProjectKeyApp([]);
  expect(await ui.cantExportMsg.find()).toBeInTheDocument();
  expect(ui.importBtn.query()).not.toBeInTheDocument();
  expect(ui.exportBtn.query()).not.toBeInTheDocument();
  expect(ui.disabledImportFeatureMsg.get()).toBeInTheDocument();
});

function renderProjectKeyApp(featureList: Feature[] = []) {
  return renderAppWithComponentContext(
    'import_export',
    routes,
    { featureList },
    { component: mockComponent({ key: COMPONENT_KEY }) },
  );
}
