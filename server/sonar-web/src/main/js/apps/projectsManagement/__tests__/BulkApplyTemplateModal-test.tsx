/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
/* eslint-disable import/order */
import * as React from 'react';
import { mount, shallow } from 'enzyme';
import BulkApplyTemplateModal, { Props } from '../BulkApplyTemplateModal';
import { click, waitAndUpdate } from '../../../helpers/testUtils';
import { parseDate } from '../../../helpers/dates';

jest.mock('../../../api/permissions', () => ({
  bulkApplyTemplate: jest.fn(() => Promise.resolve()),
  getPermissionTemplates: jest.fn(() => Promise.resolve({ permissionTemplates: [] }))
}));

const bulkApplyTemplate = require('../../../api/permissions').bulkApplyTemplate as jest.Mock<any>;
const getPermissionTemplates = require('../../../api/permissions')
  .getPermissionTemplates as jest.Mock<any>;

beforeEach(() => {
  bulkApplyTemplate.mockClear();
  getPermissionTemplates.mockClear();
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

  click(wrapper.find('Button'));
  expect(bulkApplyTemplate).toBeCalledWith({
    analyzedBefore: '2017-04-08T00:00:00+0000',
    onProvisionedOnly: true,
    organization: 'org',
    q: 'bla',
    qualifiers: 'TRK',
    templateId: 'foo'
  });
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
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

  click(wrapper.find('Button'));
  expect(wrapper).toMatchSnapshot();
  await new Promise(setImmediate);
  expect(bulkApplyTemplate).toBeCalledWith({
    organization: 'org',
    projects: 'proj1,proj2',
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
      analyzedBefore={parseDate('2017-04-08T00:00:00.000Z')}
      onClose={jest.fn()}
      organization="org"
      provisioned={true}
      qualifier="TRK"
      query="bla"
      selection={[]}
      total={17}
      {...props}
    />
  );
}
