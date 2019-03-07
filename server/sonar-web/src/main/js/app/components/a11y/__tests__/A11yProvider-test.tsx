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
import * as React from 'react';
import { shallow } from 'enzyme';
import { A11yContextShape } from '../A11yContext';
import A11yProvider from '../A11yProvider';
import { waitAndUpdate } from '../../../../helpers/testUtils';

const link1 = { key: 'link1', label: 'Link 1', weight: 0 };
const link2 = { key: 'link2', label: 'Link 2', weight: -10 };
const link3 = { key: 'link3', label: 'Link 3', weight: 0 };

it('should allow to register new skip links', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(wrapper.state('links')).toEqual([]);

  // Check that an absence of weight is treated as "0".
  instance.addA11ySkipLink({ ...link1, weight: undefined });
  expect(wrapper.state('links')).toEqual([link1]);

  instance.addA11ySkipLink(link2);
  expect(wrapper.state('links')).toEqual([link1, link2]);
});

it('should pass the ordered links to the consumers', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  instance.setState({ links: [link1, link2, link3] });
  waitAndUpdate(wrapper);
  expect((wrapper.prop('value') as A11yContextShape).links).toEqual([link2, link1, link3]);
});

it('should allow to unregister skip links', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  instance.setState({ links: [link1, link2, link3] });

  instance.removeA11ySkipLink(link1);
  expect(wrapper.state('links')).toEqual([link2, link3]);

  instance.removeA11ySkipLink(link2);
  expect(wrapper.state('links')).toEqual([link3]);
});

function shallowRender() {
  return shallow<A11yProvider>(
    <A11yProvider>
      <div />
    </A11yProvider>
  );
}
