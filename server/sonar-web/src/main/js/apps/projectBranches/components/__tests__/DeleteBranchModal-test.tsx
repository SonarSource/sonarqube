/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
jest.mock('../../../../api/branches', () => ({ deleteBranch: jest.fn() }));

import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import DeleteBranchModal from '../DeleteBranchModal';
import { ShortLivingBranch, BranchType } from '../../../../app/types';
import { submit, doAsync, click, waitAndUpdate } from '../../../../helpers/testUtils';
import { deleteBranch } from '../../../../api/branches';

beforeEach(() => {
  (deleteBranch as jest.Mock<any>).mockClear();
});

it('renders', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ loading: true });
  expect(wrapper).toMatchSnapshot();
});

it('deletes branch', async () => {
  (deleteBranch as jest.Mock<any>).mockImplementation(() => Promise.resolve());
  const onDelete = jest.fn();
  const wrapper = shallowRender(onDelete);

  submitForm(wrapper);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
  expect(onDelete).toBeCalled();
  expect(deleteBranch).toBeCalledWith('foo', 'feature');
});

it('cancels', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender(jest.fn(), onClose);

  click(wrapper.find('a'));

  return doAsync().then(() => {
    expect(onClose).toBeCalled();
  });
});

it('stops loading on WS error', async () => {
  (deleteBranch as jest.Mock<any>).mockImplementation(() => Promise.reject(null));
  const onDelete = jest.fn();
  const wrapper = shallowRender(onDelete);

  submitForm(wrapper);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
  expect(onDelete).not.toBeCalled();
  expect(deleteBranch).toBeCalledWith('foo', 'feature');
});

function shallowRender(onDelete: () => void = jest.fn(), onClose: () => void = jest.fn()) {
  const branch: ShortLivingBranch = {
    isMain: false,
    name: 'feature',
    mergeBranch: 'master',
    type: BranchType.SHORT
  };
  const wrapper = shallow(
    <DeleteBranchModal branch={branch} component="foo" onClose={onClose} onDelete={onDelete} />
  );
  (wrapper.instance() as any).mounted = true;
  return wrapper;
}

function submitForm(wrapper: ShallowWrapper<any, any>) {
  submit(wrapper.find('form'));
  expect(wrapper.state().loading).toBe(true);
}
