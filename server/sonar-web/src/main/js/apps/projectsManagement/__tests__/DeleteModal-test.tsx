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
/* eslint-disable import/first */
jest.mock('../../../api/codescan', () => ({
  deleteBulkProjects: jest.fn(() => Promise.resolve()),
}));

import { shallow } from 'enzyme';
import * as React from 'react';
import { parseDate } from '../../../helpers/dates';
import { click } from '../../../helpers/testUtils';
import DeleteModal, { Props } from '../DeleteModal';

const deleteBulkProjects = require('../../../api/codescan').deleteBulkProjects as jest.Mock<any>;

beforeEach(() => {
  deleteBulkProjects.mockClear();
});

it('deletes all projects', async () => {
  const onConfirm = jest.fn();
  const wrapper = shallowRender({ onConfirm });
  (wrapper.instance() as DeleteModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('SubmitButton'));
  expect(wrapper).toMatchSnapshot();
  expect(deleteBulkProjects).toHaveBeenCalledWith({
    analyzedBefore: '2017-04-08T00:00:00+0000',
    onProvisionedOnly: undefined,
    q: 'bla',
    qualifiers: 'TRK',
  });

  await new Promise(setImmediate);
  expect(onConfirm).toHaveBeenCalled();
});

it('deletes selected projects', async () => {
  const onConfirm = jest.fn();
  const wrapper = shallowRender({ onConfirm, selection: ['proj1', 'proj2'] });
  (wrapper.instance() as DeleteModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('SubmitButton'));
  expect(wrapper).toMatchSnapshot();
  expect(deleteBulkProjects).toHaveBeenCalledWith({ projects: 'proj1,proj2' });

  await new Promise(setImmediate);
  expect(onConfirm).toHaveBeenCalled();
});

it('closes', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  click(wrapper.find('ResetButtonLink'));
  expect(onClose).toHaveBeenCalled();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <DeleteModal
      analyzedBefore={parseDate('2017-04-08T00:00:00.000Z')}
      onClose={jest.fn()}
      onConfirm={jest.fn()}
      provisioned={false}
      qualifier="TRK"
      query="bla"
      selection={[]}
      total={17}
      {...props}
    />
  );
}
