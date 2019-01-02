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
import { shallow } from 'enzyme';
import ModalButton from '../ModalButton';
import { click } from '../../../helpers/testUtils';

it('should open/close modal', () => {
  const wrapper = shallow(
    <ModalButton modal={({ onClose }) => <button id="js-close" onClick={onClose} type="button" />}>
      {({ onClick }) => <button id="js-open" onClick={onClick} type="button" />}
    </ModalButton>
  );

  expect(wrapper.find('#js-open').exists()).toBeTruthy();
  expect(wrapper.find('#js-close').exists()).toBeFalsy();
  click(wrapper.find('#js-open'));
  expect(wrapper.find('#js-close').exists()).toBeTruthy();
  click(wrapper.find('#js-close'));
  expect(wrapper.find('#js-close').exists()).toBeFalsy();
});
