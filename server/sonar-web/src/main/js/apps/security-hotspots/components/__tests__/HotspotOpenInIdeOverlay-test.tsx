/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { HotspotOpenInIdeOverlay } from '../HotspotOpenInIdeOverlay';

it('should render nothing with fewer than 2 IDE', () => {
  const onIdeSelected = jest.fn();
  expect(
    shallow(<HotspotOpenInIdeOverlay ides={[]} onIdeSelected={onIdeSelected} />).type()
  ).toBeNull();
  expect(
    shallow(
      <HotspotOpenInIdeOverlay
        ides={[{ port: 0, ideName: 'Polop', description: 'Plouf' }]}
        onIdeSelected={onIdeSelected}
      />
    ).type()
  ).toBeNull();
});

it('should render menu and select the right IDE', () => {
  const onIdeSelected = jest.fn();
  const ide1 = { port: 0, ideName: 'Polop', description: 'Plouf' };
  const ide2 = { port: 1, ideName: 'Foo', description: '' };
  const wrapper = shallow(
    <HotspotOpenInIdeOverlay ides={[ide1, ide2]} onIdeSelected={onIdeSelected} />
  );
  expect(wrapper).toMatchSnapshot();

  wrapper.find('a').last().simulate('click');
  expect(onIdeSelected).toHaveBeenCalledWith(ide2);
});
