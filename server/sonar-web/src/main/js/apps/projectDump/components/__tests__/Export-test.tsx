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
import { mockDumpStatus, mockDumpTask } from '../../../../helpers/testMocks';
import { TaskStatuses } from '../../../../types/tasks';
import Export from '../Export';

jest.mock('../../../../api/project-dump', () => ({
  doExport: jest.fn().mockResolvedValue({}),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('no task');
  expect(shallowRender({ status: mockDumpStatus({ canBeExported: false }) })).toMatchSnapshot(
    'cannot export'
  );
  expect(shallowRender({ task: mockDumpTask({ status: TaskStatuses.Pending }) })).toMatchSnapshot(
    'task pending'
  );
  expect(
    shallowRender({ task: mockDumpTask({ status: TaskStatuses.InProgress }) })
  ).toMatchSnapshot('task in progress');
  expect(shallowRender({ task: mockDumpTask({ status: TaskStatuses.Failed }) })).toMatchSnapshot(
    'task failed'
  );
  expect(
    shallowRender({
      status: mockDumpStatus({ exportedDump: 'dump-file' }),
      task: mockDumpTask({ status: TaskStatuses.Success }),
    })
  ).toMatchSnapshot('success');
});

function shallowRender(overrides: Partial<Export['props']> = {}) {
  return shallow<Export>(
    <Export
      componentKey="key"
      loadStatus={jest.fn()}
      status={mockDumpStatus()}
      task={undefined}
      {...overrides}
    />
  );
}
