/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import React from 'react';
import { mockProjectAlmBindingConfigurationErrors } from '../../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockTask, mockTaskWarning } from '../../../../../helpers/mocks/tasks';
import { ComponentQualifier } from '../../../../../types/component';
import { TaskStatuses } from '../../../../../types/tasks';
import RecentHistory from '../../../RecentHistory';
import ComponentNav, { ComponentNavProps } from '../ComponentNav';
import Menu from '../Menu';
import InfoDrawer from '../projectInformation/InfoDrawer';

beforeEach(() => {
  jest.clearAllMocks();
  jest.spyOn(React, 'useEffect').mockImplementationOnce(f => f());
});

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ warnings: [mockTaskWarning()] })).toMatchSnapshot('has warnings');
  expect(shallowRender({ isInProgress: true })).toMatchSnapshot('has in progress notification');
  expect(shallowRender({ isPending: true })).toMatchSnapshot('has pending notification');
  expect(shallowRender({ currentTask: mockTask({ status: TaskStatuses.Failed }) })).toMatchSnapshot(
    'has failed notification'
  );
  expect(
    shallowRender({
      projectBindingErrors: mockProjectAlmBindingConfigurationErrors()
    })
  ).toMatchSnapshot('has failed project binding');
});

it('correctly adds data to the history if there are breadcrumbs', () => {
  const key = 'foo';
  const name = 'Foo';
  const qualifier = ComponentQualifier.Portfolio;
  const spy = jest.spyOn(RecentHistory, 'add');

  shallowRender({
    component: mockComponent({
      key,
      name,
      breadcrumbs: [
        {
          key: 'bar',
          name: 'Bar',
          qualifier
        }
      ]
    })
  });

  expect(spy).toBeCalledWith(key, name, qualifier.toLowerCase());
});

it('correctly toggles the project info display', () => {
  const wrapper = shallowRender();
  expect(wrapper.find(InfoDrawer).props().displayed).toBe(false);

  wrapper
    .find(Menu)
    .props()
    .onToggleProjectInfo();
  expect(wrapper.find(InfoDrawer).props().displayed).toBe(true);

  wrapper
    .find(Menu)
    .props()
    .onToggleProjectInfo();
  expect(wrapper.find(InfoDrawer).props().displayed).toBe(false);

  wrapper
    .find(Menu)
    .props()
    .onToggleProjectInfo();
  wrapper
    .find(InfoDrawer)
    .props()
    .onClose();
  expect(wrapper.find(InfoDrawer).props().displayed).toBe(false);
});

function shallowRender(props: Partial<ComponentNavProps> = {}) {
  return shallow<ComponentNavProps>(
    <ComponentNav
      branchLikes={[]}
      component={mockComponent({
        breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }]
      })}
      currentBranchLike={undefined}
      isInProgress={false}
      isPending={false}
      onComponentChange={jest.fn()}
      onWarningDismiss={jest.fn()}
      warnings={[]}
      {...props}
    />
  );
}
