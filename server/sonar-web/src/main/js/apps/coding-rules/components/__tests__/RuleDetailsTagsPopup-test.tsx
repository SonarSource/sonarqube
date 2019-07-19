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

import { shallow } from 'enzyme';
import * as React from 'react';
import RuleDetailsTagsPopup, { Props } from '../RuleDetailsTagsPopup';

jest.mock('../../../../api/rules', () => ({
  getRuleTags: jest.fn(() => Promise.resolve(['system', 'foo', 'bar', 'not-system']))
}));

const getRuleTags = require('../../../../api/rules').getRuleTags as jest.Mock<any>;

it('should render tags', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should search tags', async () => {
  const wrapper = shallowRender();
  wrapper.prop<Function>('onSearch')('sys');
  expect(getRuleTags).toBeCalledWith({ organization: 'org', ps: 11, q: 'sys' });
  await new Promise(setImmediate);
  wrapper.update();
  // should not contain system tags
  expect(wrapper.prop('tags')).toEqual(['bar', 'not-system']);
});

it('should select & unselect tags', () => {
  const setTags = jest.fn();
  const wrapper = shallowRender({ setTags });

  wrapper.prop<Function>('onSelect')('another');
  expect(setTags).lastCalledWith(['foo', 'another']);

  wrapper.prop<Function>('onUnselect')('foo');
  expect(setTags).lastCalledWith([]);
});

function shallowRender(props?: Partial<Props>) {
  return shallow(
    <RuleDetailsTagsPopup
      organization="org"
      setTags={jest.fn()}
      sysTags={['system']}
      tags={['foo']}
      {...props}
    />
  );
}
