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
import LoginButton from '../LoginButton';

it('should display correctly as a button with icon', () => {
  const wrapper = shallow(
    <LoginButton
      appearance="primary"
      icon={<i className="my-icon" />}
      onReload={jest.fn()}
      sessionUrl={`sessions/url`}>
      Connect
    </LoginButton>
  );
  expect(wrapper).toMatchSnapshot();
});

it('should correctly open a popup', () => {
  const onReload = jest.fn();
  const open = jest.fn();
  (global as any).open = open;

  const wrapper = shallow(
    <LoginButton appearance="link" onReload={onReload} sessionUrl={`sessions/url`}>
      Connect
    </LoginButton>
  );
  expect(wrapper).toMatchSnapshot();
  (wrapper.find('WithAnalyticsContext').prop('onClick') as Function)({
    preventDefault: () => {},
    stopPropagation: () => {}
  });
  expect(open).toHaveBeenCalledWith(
    '/sessions/url?return_to=%2Fintegration%2Fbitbucketcloud%2Fafter_login',
    'Login on SonarCloud',
    'toolbar=0,status=0,width=1100,height=640'
  );
});
