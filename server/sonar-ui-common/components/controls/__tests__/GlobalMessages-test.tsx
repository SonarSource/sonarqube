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
import { matchers } from 'jest-emotion';
import * as React from 'react';
import testTheme from '../../../config/jest/testTheme';
import GlobalMessages, { GlobalMessagesProps } from '../GlobalMessages';

expect.extend(matchers);

it('should not render when no message', () => {
  expect(shallowRender({ messages: [] }).type()).toBeNull();
});

it('should render correctly with a message', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('GlobalMessage').first().dive()).toMatchSnapshot();
  expect(wrapper.find('GlobalMessage').last().dive()).toMatchSnapshot();
});

it('should render with correct css', () => {
  const wrapper = shallowRender();
  expect(wrapper.render()).toMatchSnapshot();
  expect(wrapper.find('GlobalMessage').first().render()).toHaveStyleRule(
    'background-color',
    testTheme.colors.red
  );

  expect(wrapper.find('GlobalMessage').last().render()).toHaveStyleRule(
    'background-color',
    testTheme.colors.green
  );
});

function shallowRender(props: Partial<GlobalMessagesProps> = {}) {
  return shallow(
    <GlobalMessages
      closeGlobalMessage={jest.fn()}
      messages={[
        { id: '1', level: 'ERROR', message: 'Test' },
        { id: '2', level: 'SUCCESS', message: 'Test 2' },
      ]}
      {...props}
    />
  );
}
