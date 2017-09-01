/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import Form from '../Form';

it('renders', () => {
  const foo = randomGate('foo');
  const allGates = [foo, randomGate('bar')];
  expect(shallow(<Form allGates={allGates} gate={foo} onChange={jest.fn()} />)).toMatchSnapshot();
});

it('changes quality gate', () => {
  const allGates = [randomGate('foo'), randomGate('bar')];
  const onChange = jest.fn(() => Promise.resolve());
  const wrapper = shallow(<Form allGates={allGates} onChange={onChange} />);

  wrapper.find('Select').prop<Function>('onChange')({ value: 'bar' });
  expect(onChange).lastCalledWith(undefined, 'bar');

  wrapper.setProps({ gate: randomGate('foo') });
  wrapper.find('Select').prop<Function>('onChange')({ value: 'bar' });
  expect(onChange).lastCalledWith('foo', 'bar');
});

function randomGate(id: string) {
  return {
    id,
    name: id
  };
}
