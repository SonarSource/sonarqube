/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { getApplicationDetails } from '../../../api/application';
import { mockApplication, mockApplicationProject } from '../../../helpers/mocks/application';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { Application } from '../../../types/application';
import CreateBranchForm from '../CreateBranchForm';

jest.mock('../../../api/application', () => ({
  getApplicationDetails: jest.fn().mockResolvedValue({ projects: [] }),
  addApplicationBranch: jest.fn(),
  updateApplicationBranch: jest.fn()
}));

it('Should handle submit correctly', async () => {
  const app = mockApplication();
  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  const handleClose = jest.fn();
  const handleCeate = jest.fn();
  const handleUpdate = jest.fn();
  let wrapper = shallowRender({ application: app, onClose: handleClose, onCreate: handleCeate });
  wrapper.instance().handleFormSubmit();
  await waitAndUpdate(wrapper);
  expect(handleClose).toHaveBeenCalled();
  expect(handleCeate).toHaveBeenCalled();

  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  wrapper = shallowRender({
    application: app,
    branch: { isMain: false, name: 'foo' },
    onUpdate: handleUpdate
  });
  wrapper.instance().handleFormSubmit();
  await waitAndUpdate(wrapper);
  expect(handleUpdate).toHaveBeenCalled();
});

it('Should render correctly', async () => {
  const app = mockApplication({
    projects: [
      mockApplicationProject({ key: '1', enabled: true }),
      mockApplicationProject({ key: '2', enabled: true }),
      mockApplicationProject({ key: '3', enabled: false }),
      mockApplicationProject({ enabled: false })
    ]
  });
  (getApplicationDetails as jest.Mock<Promise<Application>>).mockResolvedValueOnce(app);
  const wrapper = shallowRender({ application: app, enabledProjectsKey: ['1', '3'] });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('Should close when no response', async () => {
  const app = mockApplication();
  (getApplicationDetails as jest.Mock<Promise<Application>>).mockRejectedValueOnce(app);
  const handleClose = jest.fn();
  const wrapper = shallowRender({ application: app, onClose: handleClose });
  await waitAndUpdate(wrapper);
  expect(handleClose).toHaveBeenCalled();
});

it('Should update loading flag', () => {
  const wrapper = shallowRender();
  wrapper.setState({ loading: true });
  wrapper.instance().stopLoading();
  expect(wrapper.state().loading).toBe(false);
});

it('Should update on input event', () => {
  const wrapper = shallowRender();
  wrapper.setState({ name: '' });
  wrapper
    .instance()
    .handleInputChange(({ currentTarget: { value: 'bar' } } as any) as React.ChangeEvent<
      HTMLInputElement
    >);
  expect(wrapper.state().name).toBe('bar');
});

it('Should tell if it can submit correctly', () => {
  const wrapper = shallowRender();
  wrapper.setState({ loading: true });
  expect(wrapper.instance().canSubmit()).toBe(false);
  wrapper.setState({ loading: false, name: '' });
  expect(wrapper.instance().canSubmit()).toBe(false);
  wrapper.setState({
    loading: false,
    name: 'ok',
    selectedBranches: { foo: null },
    selected: ['foo']
  });
  expect(wrapper.instance().canSubmit()).toBe(false);
  wrapper.setState({
    loading: false,
    name: 'ok',
    selectedBranches: { foo: { label: 'foo', isMain: true, value: 'foo' } },
    selected: ['foo']
  });
  expect(wrapper.instance().canSubmit()).toBe(true);
});

function shallowRender(props: Partial<CreateBranchForm['props']> = {}) {
  return shallow<CreateBranchForm>(
    <CreateBranchForm
      application={mockApplication()}
      enabledProjectsKey={[]}
      onClose={jest.fn()}
      onCreate={jest.fn()}
      onUpdate={jest.fn()}
      {...props}
    />
  );
}
