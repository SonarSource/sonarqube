/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { mockTask } from '../../../../../helpers/mocks/tasks';
import { mockComponent } from '../../../../../helpers/testMocks';
import { TaskStatuses } from '../../../../../types/tasks';
import ComponentNavBgTaskNotif from '../ComponentNavBgTaskNotif';

jest.mock('sonar-ui-common/helpers/l10n', () => ({
  ...jest.requireActual('sonar-ui-common/helpers/l10n'),
  hasMessage: jest.fn().mockReturnValue(true)
}));

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ isPending: true })).toMatchSnapshot('pending');
  expect(
    shallowRender({
      component: mockComponent({ configuration: { showBackgroundTasks: true } }),
      isPending: true
    })
  ).toMatchSnapshot('pending for admins');
  expect(shallowRender({ isInProgress: true, isPending: true })).toMatchSnapshot('in progress');
  expect(
    shallowRender({
      currentTask: mockTask({
        status: TaskStatuses.Failed,
        errorType: 'LICENSING',
        errorMessage: 'Foo'
      })
    })
  ).toMatchSnapshot('license issue');
  expect(
    shallowRender({
      currentTask: mockTask({ branch: 'my/branch', status: TaskStatuses.Failed }),
      currentTaskOnSameBranch: false
    })
  ).toMatchSnapshot('branch');
  expect(
    shallowRender({
      currentTask: mockTask({
        pullRequest: '650',
        pullRequestTitle: 'feature/my_pr',
        status: TaskStatuses.Failed
      }),
      currentTaskOnSameBranch: false
    })
  ).toMatchSnapshot('pul request');
  expect(shallowRender({ currentTask: undefined })).toMatchSnapshot('no current task');
});

function shallowRender(props: Partial<ComponentNavBgTaskNotif['props']> = {}) {
  return shallow<ComponentNavBgTaskNotif>(
    <ComponentNavBgTaskNotif
      component={mockComponent()}
      currentTask={mockTask({ status: TaskStatuses.Failed })}
      {...props}
    />
  );
}
