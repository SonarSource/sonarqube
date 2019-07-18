/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { BackgroundTasksApp } from '../BackgroundTasksApp';

jest.mock('../../../../api/ce', () => ({
  getTypes: jest.fn().mockResolvedValue({
    taskTypes: ['REPORT', 'PROJECT_EXPORT', 'PROJECT_IMPORT', 'VIEW_REFRESH']
  }),
  getActivity: jest.fn().mockResolvedValue({
    tasks: [
      {
        id: 'AWkGcOThOiAPiP5AE-kM',
        type: 'VIEW_REFRESH',
        componentId: 'AWBLZYhGOUrjxRA-u6ex',
        componentKey: 'sonar-csharp',
        componentName: 'SonarC#',
        componentQualifier: 'APP',
        status: 'FAILED',
        submittedAt: '2019-02-19T16:47:35+0100',
        startedAt: '2019-02-19T16:47:36+0100',
        executedAt: '2019-02-19T16:47:36+0100',
        executionTimeMs: 16,
        logs: false,
        errorMessage:
          'Analyses suspended. Please set a valid license for the Edition you installed.',
        hasScannerContext: false,
        organization: 'default-organization',
        errorType: 'LICENSING',
        warningCount: 0,
        warnings: []
      },
      {
        id: 'AWkGcOThOiAPiP5AE-kL',
        type: 'VIEW_REFRESH',
        componentId: 'AV2ZaHs1Wa2znA6pDz1l',
        componentKey: 'c-cpp-build-wrapper',
        componentName: 'C/C++ Build Wrapper',
        componentQualifier: 'APP',
        status: 'SUCCESS',
        submittedAt: '2019-02-19T16:47:35+0100',
        startedAt: '2019-02-19T16:47:36+0100',
        executedAt: '2019-02-19T16:47:36+0100',
        executionTimeMs: 19,
        logs: false,
        hasScannerContext: false,
        organization: 'default-organization',
        warningCount: 0,
        warnings: []
      }
    ]
  }),
  getStatus: jest.fn().mockResolvedValue({ pending: 0, failing: 15, inProgress: 0 }),
  cancelAllTasks: jest.fn().mockResolvedValue({}),
  cancelTask: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<BackgroundTasksApp['props']> = {}) {
  return shallow(
    <BackgroundTasksApp
      component={{ key: 'foo', id: '564' }}
      fetchOrganizations={jest.fn()}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
