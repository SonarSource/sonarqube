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
import * as React from 'react';
import { shallow } from 'enzyme';
import BranchRow from '../BranchRow';
import { MainBranch, ShortLivingBranch, BranchType } from '../../../../app/types';
import { click } from '../../../../helpers/testUtils';

const mainBranch: MainBranch = { isMain: true, name: 'master' };

const shortBranch: ShortLivingBranch = {
  analysisDate: '2017-09-27T00:05:19+0000',
  isMain: false,
  name: 'feature',
  mergeBranch: 'foo',
  type: BranchType.SHORT
};

it('renders main branch', () => {
  expect(shallowRender(mainBranch)).toMatchSnapshot();
});

it('renders short-living branch', () => {
  expect(shallowRender(shortBranch)).toMatchSnapshot();
});

it('renames main branch', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender(mainBranch, onChange);

  click(wrapper.find('.js-rename'));
  (wrapper.find('RenameBranchModal').prop('onRename') as Function)();
  expect(onChange).toBeCalled();
});

it('deletes short-living branch', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender(shortBranch, onChange);

  click(wrapper.find('.js-delete'));
  (wrapper.find('DeleteBranchModal').prop('onDelete') as Function)();
  expect(onChange).toBeCalled();
});

function shallowRender(branch: MainBranch | ShortLivingBranch, onChange: () => void = jest.fn()) {
  const wrapper = shallow(<BranchRow branch={branch} component="foo" onChange={onChange} />);
  (wrapper.instance() as any).mounted = true;
  return wrapper;
}
