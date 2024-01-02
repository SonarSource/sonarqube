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
import Import from '../Import';

jest.mock('../../../../api/project-dump', () => ({
  doImport: jest.fn().mockResolvedValue({}),
}));

it('should render correctly', () => {
  expect(
    shallowRender({ status: mockDumpStatus({ dumpToImport: 'import-file.zip' }) })
  ).toMatchSnapshot('import form');
  expect(shallowRender()).toMatchSnapshot('no dump to import');
  expect(shallowRender({ status: mockDumpStatus({ canBeImported: false }) })).toMatchSnapshot(
    'cannot import'
  );
  expect(shallowRender({ task: mockDumpTask({ status: TaskStatuses.Success }) })).toMatchSnapshot(
    'success'
  );
  expect(
    shallowRender({
      analysis: mockDumpTask(),
      task: mockDumpTask({ status: TaskStatuses.Success }),
    })
  ).toMatchSnapshot('success, but with analysis -> show form');
  expect(shallowRender({ task: mockDumpTask({ status: TaskStatuses.Pending }) })).toMatchSnapshot(
    'pending'
  );
  expect(
    shallowRender({ task: mockDumpTask({ status: TaskStatuses.InProgress }) })
  ).toMatchSnapshot('in progress');
  expect(shallowRender({ task: mockDumpTask({ status: TaskStatuses.Failed }) })).toMatchSnapshot(
    'failed'
  );
  expect(shallowRender({ importEnabled: false })).toMatchSnapshot('import disabled');
});

function shallowRender(overrides: Partial<Import['props']> = {}) {
  return shallow<Import>(
    <Import
      importEnabled={true}
      analysis={undefined}
      componentKey="key"
      loadStatus={jest.fn()}
      status={mockDumpStatus()}
      task={undefined}
      {...overrides}
    />
  );
}
