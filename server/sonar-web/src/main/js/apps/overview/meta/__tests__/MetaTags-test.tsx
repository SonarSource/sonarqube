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
import { click } from '../../../../helpers/testUtils';
import MetaTags from '../MetaTags';

const component = {
  key: 'my-project',
  tags: [],
  configuration: {
    showSettings: false
  },
  organization: 'foo',
  qualifier: 'TRK',
  name: 'MyProject',
  breadcrumbs: []
};

const componentWithTags = {
  key: 'my-second-project',
  tags: ['foo', 'bar'],
  configuration: {
    showSettings: true
  },
  organization: 'foo',
  qualifier: 'TRK',
  name: 'MySecondProject',
  breadcrumbs: []
};

it('should render without tags and admin rights', () => {
  expect(
    shallow(<MetaTags component={component} onComponentChange={jest.fn()} />, {
      disableLifecycleMethods: true
    })
  ).toMatchSnapshot();
});

it('should render with tags and admin rights', () => {
  expect(
    shallow(<MetaTags component={componentWithTags} onComponentChange={jest.fn()} />, {
      disableLifecycleMethods: true
    })
  ).toMatchSnapshot();
});

it('should open the tag selector on click', () => {
  const wrapper = shallow(
    <MetaTags component={componentWithTags} onComponentChange={jest.fn()} />,
    {
      disableLifecycleMethods: true
    }
  );
  expect(wrapper).toMatchSnapshot();

  // open
  click(wrapper.find('button'));
  expect(wrapper).toMatchSnapshot();

  // close
  click(wrapper.find('button'));
  expect(wrapper).toMatchSnapshot();
});
