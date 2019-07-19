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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { searchProjectTags } from '../../../../api/components';
import MetaTagsSelector from '../MetaTagsSelector';

jest.mock('../../../../api/components', () => ({
  searchProjectTags: jest.fn()
}));

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  lodash.debounce = jest.fn(fn => fn);
  return lodash;
});

it('searches tags on mount', () => {
  (searchProjectTags as jest.Mock).mockImplementation(() =>
    Promise.resolve({ tags: ['foo', 'bar'] })
  );
  mount(<MetaTagsSelector project="foo" selectedTags={[]} setProjectTags={jest.fn()} />);
  expect(searchProjectTags).toBeCalledWith({ ps: 9, q: '' });
});

it('selects and deselects tags', () => {
  const setProjectTags = jest.fn();
  const wrapper = shallow(
    <MetaTagsSelector project="foo" selectedTags={['foo', 'bar']} setProjectTags={setProjectTags} />
  );

  const tagSelect: any = wrapper.find('TagsSelector');
  tagSelect.prop('onSelect')('baz');
  expect(setProjectTags).toHaveBeenLastCalledWith(['foo', 'bar', 'baz']);

  // note that the `selectedTags` is a prop and so it wasn't changed
  tagSelect.prop('onUnselect')('bar');
  expect(setProjectTags).toHaveBeenLastCalledWith(['foo']);
});
