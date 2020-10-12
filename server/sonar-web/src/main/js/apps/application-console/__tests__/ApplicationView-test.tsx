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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getApplicationDetails } from '../../../api/application';
import { mockApplication, mockApplicationProject } from '../../../helpers/mocks/application';
import { mockRouter } from '../../../helpers/testMocks';
import { Application } from '../../../types/application';
import ApplicationView from '../ApplicationView';

jest.mock('../../../api/application', () => ({
  getApplicationDetails: jest.fn().mockResolvedValue({})
}));

it('Should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('Should add project to application', async () => {
  const app = mockApplication();
  const project = mockApplicationProject({ key: 'FOO' });
  (getApplicationDetails as jest.Mock<Promise<Application>>).mockRejectedValueOnce(app);
  let wrapper = shallowRender({});
  wrapper.instance().handleAddProject(project);
  expect(wrapper.state().application?.projects).toBeUndefined();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  wrapper = shallowRender({});
  await waitAndUpdate(wrapper);
  wrapper.instance().handleAddProject(project);
  expect(wrapper.state().application?.projects).toContain(project);
});

it('Should remove project from application', async () => {
  const project = mockApplicationProject({ key: 'FOO' });
  const app = mockApplication({ projects: [project] });

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockRejectedValueOnce(app);
  let wrapper = shallowRender({});
  wrapper.instance().handleRemoveProject('FOO');
  expect(wrapper.state().application?.projects).toBeUndefined();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  wrapper = shallowRender({});
  await waitAndUpdate(wrapper);
  wrapper.instance().handleRemoveProject('FOO');
  expect(wrapper.state().application?.projects.length).toBe(0);
});

it('Should edit application correctly', async () => {
  const app = mockApplication();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockRejectedValueOnce(app);
  let wrapper = shallowRender({});
  wrapper.instance().handleEdit(app.key, 'NEW_NAME', 'NEW_DESC');
  expect(wrapper.state().application).toBeUndefined();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  wrapper = shallowRender({});
  await waitAndUpdate(wrapper);
  wrapper.instance().handleEdit(app.key, 'NEW_NAME', 'NEW_DESC');
  expect(wrapper.state().application?.name).toBe('NEW_NAME');
  expect(wrapper.state().application?.description).toBe('NEW_DESC');
});

it('Should update branch correctly', async () => {
  const app = mockApplication();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockRejectedValueOnce(app);
  let wrapper = shallowRender({});
  wrapper.instance().handleUpdateBranches([]);
  expect(wrapper.state().application).toBeUndefined();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  wrapper = shallowRender({});
  await waitAndUpdate(wrapper);
  wrapper.instance().handleUpdateBranches([]);
  expect(wrapper.state().application?.branches.length).toBe(0);
});

function shallowRender(props: Partial<ApplicationView['props']> = {}) {
  return shallow<ApplicationView>(
    <ApplicationView
      applicationKey={'1'}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      pathname={'test'}
      router={mockRouter()}
      {...props}
    />
  );
}
