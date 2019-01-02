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
import { click } from '../../../../helpers/testUtils';
import LineDuplicationBlock from '../LineDuplicationBlock';

it('render duplicated line', () => {
  const line = { line: 3, duplicated: true };
  const onPopupToggle = jest.fn();
  const wrapper = shallow(
    <LineDuplicationBlock
      duplicated={true}
      index={1}
      line={line}
      onPopupToggle={onPopupToggle}
      popupOpen={false}
      renderDuplicationPopup={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('[tabIndex]'));
  expect(onPopupToggle).toHaveBeenCalled();
});

it('render not duplicated line', () => {
  const line = { line: 3, duplicated: false };
  const wrapper = shallow(
    <LineDuplicationBlock
      duplicated={false}
      index={1}
      line={line}
      onPopupToggle={jest.fn()}
      popupOpen={false}
      renderDuplicationPopup={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
