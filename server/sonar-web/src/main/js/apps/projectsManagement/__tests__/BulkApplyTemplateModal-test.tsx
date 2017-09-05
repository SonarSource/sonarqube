/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
jest.mock('../../../api/permissions', () => ({
  applyTemplateToProject: jest.fn(),
  bulkApplyTemplate: jest.fn(),
  getPermissionTemplates: jest.fn()
}));

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import BulkApplyTemplateModal, { Props } from '../BulkApplyTemplateModal';
import { Type } from '../utils';
import { click } from '../../../helpers/testUtils';

const applyTemplateToProject = require('../../../api/permissions')
  .applyTemplateToProject as jest.Mock<any>;
const bulkApplyTemplate = require('../../../api/permissions').bulkApplyTemplate as jest.Mock<any>;
const getPermissionTemplates = require('../../../api/permissions')
  .getPermissionTemplates as jest.Mock<any>;

beforeEach(() => {
  applyTemplateToProject.mockImplementation(() => Promise.resolve()).mockClear();
  bulkApplyTemplate.mockImplementation(() => Promise.resolve()).mockClear();
  getPermissionTemplates
    .mockImplementation(() => Promise.resolve({ permissionTemplates: [] }))
    .mockClear();
});

it('fetches permission templates on mount', () => {
  mount(render());
  expect(getPermissionTemplates).toBeCalledWith('org');
});

it('bulk applies template to all results', async () => {
  const wrapper = shallow(render());
  (wrapper.instance() as BulkApplyTemplateModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({
    loading: false,
    permissionTemplate: 'foo',
    permissionTemplates: [{ id: 'foo', name: 'Foo' }, { id: 'bar', name: 'Bar' }]
  });
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('button'));
  expect(bulkApplyTemplate).toBeCalledWith({
    organization: 'org',
    q: 'bla',
    qualifier: 'TRK',
    templateId: 'foo'
  });
  expect(wrapper).toMatchSnapshot();

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('bulk applies template to selected results', async () => {
  const wrapper = shallow(render({ selection: ['proj1', 'proj2'] }));
  (wrapper.instance() as BulkApplyTemplateModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({
    loading: false,
    permissionTemplate: 'foo',
    permissionTemplates: [{ id: 'foo', name: 'Foo' }, { id: 'bar', name: 'Bar' }]
  });
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('button'));
  expect(wrapper).toMatchSnapshot();
  await new Promise(setImmediate);
  expect(applyTemplateToProject.mock.calls).toHaveLength(2);
  expect(applyTemplateToProject).toBeCalledWith({
    organization: 'org',
    projectKey: 'proj1',
    templateId: 'foo'
  });
  expect(applyTemplateToProject).toBeCalledWith({
    organization: 'org',
    projectKey: 'proj2',
    templateId: 'foo'
  });

  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('closes', () => {
  const onClose = jest.fn();
  const wrapper = shallow(render({ onClose }));
  click(wrapper.find('.js-modal-close'));
  expect(onClose).toBeCalled();
});

function render(props?: { [P in keyof Props]?: Props[P] }) {
  return (
    <BulkApplyTemplateModal
      onClose={jest.fn()}
      organization="org"
      qualifier="TRK"
      query="bla"
      selection={[]}
      total={17}
      type={Type.All}
      {...props}
    />
  );
}
