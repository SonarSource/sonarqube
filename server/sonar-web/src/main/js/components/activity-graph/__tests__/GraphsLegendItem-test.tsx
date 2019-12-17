/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { ClearButton } from 'sonar-ui-common/components/controls/buttons';
import { click } from 'sonar-ui-common/helpers/testUtils';
import GraphsLegendItem from '../GraphsLegendItem';

it('should render correctly a legend', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      className: 'myclass',
      index: 1,
      metric: 'foo',
      name: 'Foo',
      removeMetric: jest.fn()
    })
  ).toMatchSnapshot('with legend');
  expect(shallowRender({ showWarning: true })).toMatchSnapshot('with warning');
});

it('should correctly handle clicks', () => {
  const removeMetric = jest.fn();
  const wrapper = shallowRender({ removeMetric });
  click(wrapper.find(ClearButton));
  expect(removeMetric).toBeCalledWith('bugs');
});

function shallowRender(props: Partial<GraphsLegendItem['props']> = {}) {
  return shallow<GraphsLegendItem>(
    <GraphsLegendItem index={2} metric="bugs" name="Bugs" {...props} />
  );
}
