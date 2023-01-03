/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockApplication, mockApplicationProject } from '../../../helpers/mocks/application';
import { mockBranch, mockMainBranch } from '../../../helpers/mocks/branch-like';
import ApplicationBranches from '../ApplicationBranches';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      application: mockApplication({ projects: [mockApplicationProject({ enabled: true })] })
    })
  ).toMatchSnapshot('can create branches');
  const wrapper = shallowRender();
  wrapper.setState({ creating: true });
  expect(wrapper).toMatchSnapshot('creating branch');
});

it('correctly triggers the onUpdateBranches prop', () => {
  const onUpdateBranches = jest.fn();
  const branch = mockBranch();
  const branches = [mockMainBranch()];
  const wrapper = shallowRender({ application: mockApplication({ branches }), onUpdateBranches });
  const instance = wrapper.instance();

  instance.handleCreateClick();
  expect(wrapper.state().creating).toBe(true);

  instance.handleCreateFormClose();
  expect(wrapper.state().creating).toBe(false);

  instance.handleCreate(branch);
  expect(onUpdateBranches).toBeCalledWith([...branches, branch]);
});

function shallowRender(props: Partial<ApplicationBranches['props']> = {}) {
  return shallow<ApplicationBranches>(
    <ApplicationBranches application={mockApplication()} onUpdateBranches={jest.fn()} {...props} />
  );
}
