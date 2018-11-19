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
import Form from '../Form';

it('renders', () => {
  const foo = randomGate(1);
  const allGates = [foo, randomGate(2)];
  expect(shallow(<Form allGates={allGates} gate={foo} onChange={jest.fn()} />)).toMatchSnapshot();
});

it('changes quality gate', () => {
  const allGates = [randomGate(1), randomGate(2)];
  const onChange = jest.fn(() => Promise.resolve());
  const wrapper = shallow(<Form allGates={allGates} onChange={onChange} />);

  wrapper.find('Select').prop<Function>('onChange')({ value: 2 });
  expect(onChange).lastCalledWith(undefined, 2);

  wrapper.setProps({ gate: randomGate(1) });
  wrapper.find('Select').prop<Function>('onChange')({ value: 2 });
  expect(onChange).lastCalledWith(1, 2);
});

function randomGate(id: number) {
  return {
    id,
    name: `name-${id}`
  };
}
