/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import DisableableSelectOption, { DisableableSelectOptionProps } from '../DisableableSelectOption';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ option: { value: 'baz' } })).toMatchSnapshot('no label');
  expect(
    shallowRender({ option: { label: 'Bar', value: 'bar', isDisabled: true } })
  ).toMatchSnapshot('disabled');
  expect(
    shallowRender({
      option: { label: 'Bar', value: 'bar', isDisabled: true },
      disabledReason: 'bar baz',
    })
  ).toMatchSnapshot('disabled, with explanation');
});

function shallowRender(props: Partial<DisableableSelectOptionProps> = {}) {
  return shallow<DisableableSelectOptionProps>(
    <DisableableSelectOption
      option={{ label: 'Foo', value: 'foo' }}
      disableTooltipOverlay={() => 'foo bar'}
      {...props}
    />
  );
}
