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
import InfoDrawerPage, { InfoDrawerPageProps } from '../InfoDrawerPage';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ displayed: true })).toMatchSnapshot();
});

it('should call onPageChange when clicked', () => {
  const onPageChange = jest.fn();
  const wrapper = shallowRender({ onPageChange, displayed: true });

  wrapper.find('.back-button').simulate('click');

  expect(onPageChange).toHaveBeenCalledTimes(1);
});

function shallowRender(props: Partial<InfoDrawerPageProps> = {}) {
  return shallow(
    <InfoDrawerPage displayed={false} onPageChange={jest.fn()} {...props}>
      <div>content</div>
    </InfoDrawerPage>
  );
}
