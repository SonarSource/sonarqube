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
import * as React from 'react';
import { shallow, mount, ShallowWrapper } from 'enzyme';
import Dropdown, { DropdownOverlay } from '../Dropdown';
import { Button } from '../../ui/buttons';
import { click } from '../../../helpers/testUtils';
import { PopupPlacement } from '../../ui/popups';

describe('Dropdown', () => {
  it('renders', () => {
    expect(
      shallow(<Dropdown overlay={<div id="overlay" />}>{() => <div />}</Dropdown>)
        .find('div')
        .exists()
    ).toBeTruthy();
  });

  it('toggles with element child', () => {
    checkToggle(
      shallow(
        <Dropdown overlay={<div id="overlay" />}>
          <Button />
        </Dropdown>
      )
    );

    checkToggle(
      shallow(
        <Dropdown overlay={<div id="overlay" />}>
          <a href="#">click me!</a>
        </Dropdown>
      ),
      'a'
    );
  });

  it('toggles with render prop', () => {
    checkToggle(
      shallow(
        <Dropdown overlay={<div id="overlay" />}>
          {({ onToggleClick }) => <Button onClick={onToggleClick} />}
        </Dropdown>
      )
    );
  });

  it('should call onOpen', () => {
    const onOpen = jest.fn();
    const wrapper = mount(
      <Dropdown onOpen={onOpen} overlay={<div id="overlay" />}>
        <Button />
      </Dropdown>
    );
    expect(onOpen).not.toBeCalled();
    click(wrapper.find('Button'));
    expect(onOpen).toBeCalled();
  });

  function checkToggle(wrapper: ShallowWrapper, selector = 'Button') {
    expect(wrapper.state()).toEqual({ open: false });

    click(wrapper.find(selector));
    expect(wrapper.state()).toEqual({ open: true });

    click(wrapper.find(selector));
    expect(wrapper.state()).toEqual({ open: false });
  }
});

describe('DropdownOverlay', () => {
  it('should render overlay with screen fixer', () => {
    const wrapper = shallow(
      <DropdownOverlay>
        <div />
      </DropdownOverlay>,
      // disable ScreenPositionFixer positioning
      { disableLifecycleMethods: true }
    );
    expect(wrapper.is('ScreenPositionFixer')).toBe(true);
    expect(wrapper.dive().is('Popup')).toBe(true);
  });

  it('should render overlay without screen fixer', () => {
    const wrapper = shallow(
      <DropdownOverlay placement={PopupPlacement.BottomRight}>
        <div />
      </DropdownOverlay>
    );
    expect(wrapper.is('Popup')).toBe(true);
  });
});
