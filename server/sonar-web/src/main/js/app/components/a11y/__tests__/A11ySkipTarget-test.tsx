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
import { mount } from 'enzyme';
import { A11ySkipTargetInner } from '../A11ySkipTarget';

it('should render correctly, and (un)register the link when (un)mounted', () => {
  const link = { key: 'main', label: 'Skip to content' };
  const addA11ySkipLink = jest.fn();
  const removeA11ySkipLink = jest.fn();
  const wrapper = mount(
    <A11ySkipTargetInner
      addA11ySkipLink={addA11ySkipLink}
      anchor={link.key}
      label={link.label}
      removeA11ySkipLink={removeA11ySkipLink}
    />
  );

  expect(wrapper).toMatchSnapshot();
  expect(addA11ySkipLink).toBeCalledWith(link);
  wrapper.unmount();
  expect(removeA11ySkipLink).toBeCalledWith(link);
});
