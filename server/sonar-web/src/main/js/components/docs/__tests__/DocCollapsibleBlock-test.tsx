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
import DocCollapsibleBlock from '../DocCollapsibleBlock';

const children = (
  <div>
    <h2>Foo</h2>
    <p>Bar</p>
  </div>
);

it('should render a collapsible block', () => {
  const wrapper = shallow(<DocCollapsibleBlock>{children}</DocCollapsibleBlock>);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('a'));
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should not render if not at least 2 children', () => {
  const wrapper = shallow(
    <DocCollapsibleBlock>
      <div>foobar</div>
    </DocCollapsibleBlock>
  );
  expect(wrapper).toMatchSnapshot();
});
