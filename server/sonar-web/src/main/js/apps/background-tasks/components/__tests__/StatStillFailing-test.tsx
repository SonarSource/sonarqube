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
import StatStillFailing, { Props } from '../StatStillFailing';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should not render', () => {
  expect(shallowRender({ failingCount: undefined }).type()).toBeNull();
});

it('should render without the filter link', () => {
  expect(shallowRender({ failingCount: 0 })).toMatchSnapshot();
});

it('should trigger filtering failures', () => {
  const onShowFailing = jest.fn();
  const result = shallowRender({ onShowFailing });
  expect(onShowFailing).not.toBeCalled();
  click(result.find('ButtonLink'));
  expect(onShowFailing).toBeCalled();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(<StatStillFailing failingCount={5} onShowFailing={jest.fn()} {...props} />);
}
