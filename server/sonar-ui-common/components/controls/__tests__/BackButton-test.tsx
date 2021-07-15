/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import testTheme from '../../../config/jest/testTheme';
import { click } from '../../../helpers/testUtils';
import { ThemeProvider } from '../../theme';
import BackButton from '../BackButton';

it('should render properly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('ContextConsumer').dive()).toMatchSnapshot();
});

it('should handle click', () => {
  const onClick = jest.fn();
  const wrapper = shallowRender({ onClick });
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('a'));
  expect(onClick).toBeCalled();
});

function shallowRender(props: Partial<BackButton['props']> = {}) {
  return shallow<BackButton>(<BackButton onClick={jest.fn()} {...props} />, {
    wrappingComponent: ThemeProvider,
    wrappingComponentProps: {
      theme: testTheme,
    },
  });
}
