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
import RadioToggle from '../RadioToggle';
import { change } from '../../../helpers/testUtils';

it('renders', () => {
  expect(shallow(getSample())).toMatchSnapshot();
});

it('calls onCheck', () => {
  const onCheck = jest.fn();
  const wrapper = shallow(getSample({ onCheck }));
  change(wrapper.find('input[value="two"]'), 'two');
  expect(onCheck).toBeCalledWith('two');
});

it('accepts advanced options fields', () => {
  expect(
    shallow(
      getSample({
        options: [
          { value: 'one', label: 'first', tooltip: 'foo' },
          { value: 'two', label: 'second', tooltip: 'bar', disabled: true }
        ]
      })
    )
  ).toMatchSnapshot();
});

function getSample(props?: any) {
  const options = [{ value: 'one', label: 'first' }, { value: 'two', label: 'second' }];
  return <RadioToggle options={options} name="sample" onCheck={() => true} {...props} />;
}
