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
import { click } from 'sonar-ui-common/helpers/testUtils';
import MenuBlock from '../MenuBlock';

it('should render a closed menu block', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render an opened menu block', () => {
  expect(shallowRender({ openByDefault: true })).toMatchSnapshot();
});

it('should not render a high depth differently than a depth of 3', () => {
  expect(
    shallowRender({ block: { title: 'Foo', children: ['/foo'] }, depth: 6 })
  ).toMatchSnapshot();
});

it('can be opened and closed', () => {
  const wrapper = shallowRender();
  expect(wrapper.state('open')).toBe(false);
  click(wrapper.find('ButtonLink'));
  expect(wrapper.state('open')).toBe(true);
});

function shallowRender(props: Partial<MenuBlock['props']> = {}) {
  return shallow(
    <MenuBlock
      block={{
        title: 'Foo',
        children: [
          '/bar/',
          '/baz/',
          {
            title: 'Baz',
            children: ['/baz/foo']
          },
          {
            title: 'Bar',
            url: 'http://example.com'
          }
        ]
      }}
      openByDefault={false}
      openChain={[]}
      pages={[
        {
          content: 'bar',
          relativeName: '/bar/',
          text: 'bar',
          title: 'Bar',
          navTitle: undefined,
          url: '/bar/'
        },
        {
          content: 'baz',
          relativeName: '/baz/',
          text: 'baz',
          title: 'baz',
          navTitle: 'baznav',
          url: '/baz/'
        }
      ]}
      splat="/foo/"
      title="Foo"
      {...props}
    />
  );
}
