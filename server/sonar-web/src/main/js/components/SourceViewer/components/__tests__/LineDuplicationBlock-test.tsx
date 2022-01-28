/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import Toggler from '../../../../components/controls/Toggler';
import { click } from '../../../../helpers/testUtils';
import { LineDuplicationBlock, LineDuplicationBlockProps } from '../LineDuplicationBlock';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ line: { line: 3, duplicated: false }, duplicated: false })
  ).toMatchSnapshot('not duplicated');
});

it('should correctly open/close the dropdown', () => {
  const wrapper = shallowRender();
  click(wrapper.find('div[role="button"]'));
  expect(wrapper.find(Toggler).prop('open')).toBe(true);
  wrapper.find(Toggler).prop('onRequestClose')();
  expect(wrapper.find(Toggler).prop('open')).toBe(false);
});

it('should correctly call the onCick prop', () => {
  const line = { line: 1, duplicated: true };
  const onClick = jest.fn();
  const wrapper = shallowRender({ line, onClick });

  // Propagate if blocks aren't loaded.
  click(wrapper.find('div[role="button"]'));
  expect(onClick).toBeCalledWith(line);

  // Don't propagate if blocks were loaded.
  onClick.mockClear();
  wrapper.setProps({ blocksLoaded: true });
  click(wrapper.find('div[role="button"]'));
  expect(onClick).not.toBeCalled();
});

function shallowRender(props: Partial<LineDuplicationBlockProps> = {}) {
  return shallow<LineDuplicationBlockProps>(
    <LineDuplicationBlock
      blocksLoaded={false}
      duplicated={true}
      index={1}
      line={{ line: 3, duplicated: true }}
      renderDuplicationPopup={jest.fn()}
      {...props}
    />
  );
}
