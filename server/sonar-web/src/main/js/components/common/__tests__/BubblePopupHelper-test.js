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
import { shallow, mount } from 'enzyme';
import React from 'react';
import BubblePopupHelper from '../BubblePopupHelper';
import BubblePopup from '../BubblePopup';
import { click } from '../../../helpers/testUtils';

it('should render an open popup on the right', () => {
  const toggle = jest.fn();
  const popup = shallow(
    <BubblePopupHelper
      isOpen={true}
      position="bottomright"
      togglePopup={toggle}
      popup={
        <BubblePopup>
          <span>test</span>
        </BubblePopup>
      }>
      <button onClick={toggle}>open</button>
    </BubblePopupHelper>,
    { disableLifecycleMethods: true }
  );
  expect(popup).toMatchSnapshot();
});

it('should render the popup helper with a closed popup', () => {
  const toggle = jest.fn();
  const popup = shallow(
    <BubblePopupHelper
      isOpen={false}
      position="bottomright"
      togglePopup={toggle}
      popup={
        <BubblePopup>
          <span>test</span>
        </BubblePopup>
      }>
      <button onClick={toggle}>open</button>
    </BubblePopupHelper>,
    { disableLifecycleMethods: true }
  );
  expect(popup).toMatchSnapshot();
});

it('should render with custom classes', () => {
  const toggle = jest.fn();
  const popup = shallow(
    <BubblePopupHelper
      customClass="myhelperclass"
      isOpen={true}
      position="bottomright"
      togglePopup={toggle}
      popup={
        <BubblePopup customClass="mypopupclass">
          <span>test</span>
        </BubblePopup>
      }>
      <button onClick={toggle}>open</button>
    </BubblePopupHelper>,
    { disableLifecycleMethods: true }
  );
  expect(popup).toMatchSnapshot();
});

it('should render the popup with offset', () => {
  const toggle = jest.fn();
  const popup = mount(
    <BubblePopupHelper
      isOpen={true}
      offset={{ vertical: 5, horizontal: 2 }}
      position="bottomright"
      togglePopup={toggle}
      popup={
        <BubblePopup>
          <span>test</span>
        </BubblePopup>
      }>
      <button onClick={toggle}>open</button>
    </BubblePopupHelper>
  );
  expect(popup.find('BubblePopup')).toMatchSnapshot();
});

it('should render an open popup on the left', () => {
  const toggle = jest.fn();
  const popup = mount(
    <BubblePopupHelper
      isOpen={true}
      offset={{ vertical: 0, horizontal: 2 }}
      position="bottomleft"
      togglePopup={toggle}
      popup={
        <BubblePopup>
          <span>test</span>
        </BubblePopup>
      }>
      <button onClick={toggle}>open</button>
    </BubblePopupHelper>
  );
  expect(popup.find('BubblePopup')).toMatchSnapshot();
});

it('should correctly handle clicks on the button', () => {
  const toggle = jest.fn(() => popup.setProps({ isOpen: !popup.props().isOpen }));
  const popup = shallow(
    <BubblePopupHelper
      isOpen={false}
      offset={{ vertical: 0, horizontal: 2 }}
      position="bottomleft"
      togglePopup={toggle}
      popup={
        <BubblePopup>
          <span>test</span>
        </BubblePopup>
      }>
      <button onClick={toggle}>open</button>
    </BubblePopupHelper>,
    { disableLifecycleMethods: true }
  );
  expect(popup).toMatchSnapshot();
  click(popup.find('button'));
  expect(toggle.mock.calls.length).toBe(1);
  expect(popup).toMatchSnapshot();
});
