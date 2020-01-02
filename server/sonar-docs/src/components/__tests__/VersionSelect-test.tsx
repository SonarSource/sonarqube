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
import { click } from 'sonar-ui-common/helpers/testUtils';
import OutsideClickHandler from '../OutsideClickHandler';
import VersionSelect from '../VersionSelect';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');

  const wrapper = shallowRender();
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot('open');

  expect(shallowRender({ isOnCurrentVersion: true })).toMatchSnapshot('on current version');
});

it('should handle open/closing the list', () => {
  const wrapper = shallowRender();

  click(wrapper.find('button'));
  expect(wrapper.state().open).toBe(true);
  click(wrapper.find('button'));
  expect(wrapper.state().open).toBe(false);

  wrapper.setState({ open: true });
  wrapper.find(OutsideClickHandler).prop('onClickOutside')();
  expect(wrapper.state().open).toBe(false);
});

function shallowRender(props: Partial<VersionSelect['props']> = {}) {
  return shallow<VersionSelect>(
    <VersionSelect
      isOnCurrentVersion={false}
      selectedVersionValue="1.0"
      versions={[
        { value: '3.0', current: true },
        { value: '2.0', current: false, lts: true },
        { value: '1.0', current: false }
      ]}
      {...props}
    />
  );
}
