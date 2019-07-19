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
/* eslint-disable import/first */
jest.mock('../../../api/components', () => ({
  bulkDeleteProjects: jest.fn(() => Promise.resolve())
}));

import { shallow } from 'enzyme';
import * as React from 'react';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import { click } from 'sonar-ui-common/helpers/testUtils';
import DeleteModal, { Props } from '../DeleteModal';

const bulkDeleteProjects = require('../../../api/components').bulkDeleteProjects as jest.Mock<any>;

beforeEach(() => {
  bulkDeleteProjects.mockClear();
});

it('deletes all projects', async () => {
  const onConfirm = jest.fn();
  const wrapper = shallowRender({ onConfirm });
  (wrapper.instance() as DeleteModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('SubmitButton'));
  expect(wrapper).toMatchSnapshot();
  expect(bulkDeleteProjects).toBeCalledWith({
    analyzedBefore: '2017-04-08T00:00:00+0000',
    onProvisionedOnly: undefined,
    organization: 'org',
    q: 'bla',
    qualifiers: 'TRK'
  });

  await new Promise(setImmediate);
  expect(onConfirm).toBeCalled();
});

it('deletes selected projects', async () => {
  const onConfirm = jest.fn();
  const wrapper = shallowRender({ onConfirm, selection: ['proj1', 'proj2'] });
  (wrapper.instance() as DeleteModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('SubmitButton'));
  expect(wrapper).toMatchSnapshot();
  expect(bulkDeleteProjects).toBeCalledWith({ organization: 'org', projects: 'proj1,proj2' });

  await new Promise(setImmediate);
  expect(onConfirm).toBeCalled();
});

it('closes', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  click(wrapper.find('ResetButtonLink'));
  expect(onClose).toBeCalled();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <DeleteModal
      analyzedBefore={parseDate('2017-04-08T00:00:00.000Z')}
      onClose={jest.fn()}
      onConfirm={jest.fn()}
      organization="org"
      provisioned={false}
      qualifier="TRK"
      query="bla"
      selection={[]}
      total={17}
      {...props}
    />
  );
}
