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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { HotspotStatus } from '../../../../types/security-hotspots';
import HotspotActions, { HotspotActionsProps } from '../HotspotActions';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should open when clicked', async () => {
  const wrapper = shallowRender();

  wrapper.find(Button).simulate('click');

  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

it('should register an eventlistener', () => {
  let useEffectCleanup: void | (() => void | undefined) = () =>
    fail('useEffect should clean after itself');
  jest.spyOn(React, 'useEffect').mockImplementationOnce(f => {
    useEffectCleanup = f() || useEffectCleanup;
  });
  let listenerCallback = (_event: { key: string }) =>
    fail('Effect should have registered callback');
  const addEventListener = jest.fn((_event, callback) => {
    listenerCallback = callback;
  });
  jest.spyOn(document, 'addEventListener').mockImplementation(addEventListener);
  const removeEventListener = jest.spyOn(document, 'removeEventListener');
  const wrapper = shallowRender();

  wrapper.find(Button).simulate('click');
  expect(wrapper).toMatchSnapshot('Dropdown open');

  listenerCallback({ key: 'whatever' });
  expect(wrapper).toMatchSnapshot('Dropdown still open');

  listenerCallback({ key: 'Escape' });
  expect(wrapper).toMatchSnapshot('Dropdown closed');

  useEffectCleanup();
  expect(removeEventListener).toBeCalledWith('keydown', listenerCallback, false);
});

function shallowRender(props: Partial<HotspotActionsProps> = {}) {
  return shallow(
    <HotspotActions
      hotspotKey="key"
      hotspotStatus={HotspotStatus.TO_REVIEW}
      onSubmit={jest.fn()}
      {...props}
    />
  );
}
