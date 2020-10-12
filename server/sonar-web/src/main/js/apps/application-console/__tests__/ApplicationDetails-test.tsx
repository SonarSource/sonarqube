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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { deleteApplication, refreshApplication } from '../../../api/application';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import { mockApplication } from '../../../helpers/mocks/application';
import ApplicationDetails from '../ApplicationDetails';
import EditForm from '../EditForm';

jest.mock('../../../api/application', () => ({
  deleteApplication: jest.fn().mockResolvedValue({}),
  editApplication: jest.fn(),
  refreshApplication: jest.fn().mockResolvedValue({})
}));

jest.mock('../../../app/utils/addGlobalSuccessMessage', () => ({
  default: jest.fn()
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      application: mockApplication({ description: 'Foo bar', key: 'foo' }),
      canRecompute: true,
      single: false
    })
  ).toMatchSnapshot('can delete and recompute');
});

it('should handle editing', () => {
  const wrapper = shallowRender();
  click(wrapper.find('#view-details-edit'));
  expect(wrapper.find(EditForm)).toMatchSnapshot('edit form');
});

it('should handle deleting', async () => {
  const onDelete = jest.fn();
  const wrapper = shallowRender({ onDelete, single: false });

  wrapper.instance().handleDelete();
  expect(deleteApplication).toBeCalledWith('foo');
  await waitAndUpdate(wrapper);
  expect(onDelete).toBeCalledWith('foo');
});

it('should handle refreshing', async () => {
  const wrapper = shallowRender({ single: false });

  wrapper.instance().handleRefreshClick();
  expect(refreshApplication).toBeCalledWith('foo');
  await waitAndUpdate(wrapper);
  expect(addGlobalSuccessMessage).toBeCalled();
});

function shallowRender(props: Partial<ApplicationDetails['props']> = {}) {
  return shallow<ApplicationDetails>(
    <ApplicationDetails
      application={mockApplication({ key: 'foo' })}
      canRecompute={false}
      onAddProject={jest.fn()}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      onRemoveProject={jest.fn()}
      onUpdateBranches={jest.fn()}
      pathname="path/name"
      single={true}
      {...props}
    />
  );
}
