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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getActivity } from '../../../api/ce';
import { getStatus } from '../../../api/project-dump';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockDumpStatus, mockDumpTask } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { TaskStatuses } from '../../../types/tasks';
import { ProjectDumpApp } from '../ProjectDumpApp';

jest.mock('../../../api/ce', () => ({
  getActivity: jest.fn().mockResolvedValue({ tasks: [] }),
}));

jest.mock('../../../api/project-dump', () => ({
  getStatus: jest.fn().mockResolvedValue({}),
}));

beforeEach(() => {
  jest.useFakeTimers();
  jest.clearAllMocks();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('should render correctly', async () => {
  (getActivity as jest.Mock)
    .mockResolvedValueOnce({ tasks: [mockDumpTask()] })
    .mockResolvedValueOnce({ tasks: [mockDumpTask()] })
    .mockResolvedValueOnce({ tasks: [mockDumpTask()] });

  let wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');

  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot('loaded');

  wrapper = shallowRender({ hasFeature: jest.fn().mockReturnValue(false) });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('loaded without import');
});

it('should poll for task status update', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  jest.clearAllMocks();

  const finalStatus = mockDumpStatus({ exportedDump: 'export-path' });
  (getStatus as jest.Mock)
    .mockResolvedValueOnce(mockDumpStatus())
    .mockResolvedValueOnce(finalStatus);
  (getActivity as jest.Mock)
    .mockResolvedValueOnce({ tasks: [mockDumpTask({ status: TaskStatuses.Pending })] })
    .mockResolvedValueOnce({ tasks: [mockDumpTask({ status: TaskStatuses.Success })] });

  wrapper.instance().poll();

  // wait for all promises
  await waitAndUpdate(wrapper);
  jest.runAllTimers();
  await waitAndUpdate(wrapper);

  expect(getStatus).toHaveBeenCalledTimes(2);
  expect(wrapper.state().status).toBe(finalStatus);
});

function shallowRender(overrides: Partial<ProjectDumpApp['props']> = {}) {
  return shallow<ProjectDumpApp>(
    <ProjectDumpApp
      hasFeature={jest.fn().mockReturnValue(true)}
      component={mockComponent()}
      {...overrides}
    />
  );
}
