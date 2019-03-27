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
import MetaLink from '../MetaLink';
import { click } from '../../../../helpers/testUtils';

it('should match snapshot', () => {
  const link = {
    id: '1',
    name: 'Foo',
    url: 'http://example.com',
    type: 'foo'
  };

  expect(shallow(<MetaLink link={link} />)).toMatchSnapshot();
  expect(shallow(<MetaLink iconOnly={true} link={link} />)).toMatchSnapshot();
});

it('should render dangerous links as plaintext', () => {
  const link = {
    id: '1',
    name: 'Dangerous',
    url: 'javascript:alert("hi")',
    type: 'dangerous'
  };

  expect(shallow(<MetaLink link={link} />)).toMatchSnapshot();
});

it('should expand and collapse link', () => {
  const link = {
    id: '1',
    name: 'Foo',
    url: 'scm:git:git@github.com',
    type: 'foo'
  };

  const wrapper = shallow(<MetaLink link={link} />);
  expect(wrapper).toMatchSnapshot();

  // expand
  click(wrapper.find('a'));
  expect(wrapper).toMatchSnapshot();

  // collapse
  click(wrapper.find('a'));
  expect(wrapper).toMatchSnapshot();
});
