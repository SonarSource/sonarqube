/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import BoxedTabs, { BoxedTabsProps } from '../BoxedTabs';

it('should render correctly', () => {
  expect(mountRender()).toMatchSnapshot();
});

it('should call onSelect when a tab is clicked', () => {
  const onSelect = jest.fn();
  const wrapper = shallowRender({ onSelect });

  wrapper.find('Styled(button)').get(1).props.onClick();

  expect(onSelect).toHaveBeenCalledWith('b');
});

function shallowRender(overrides: Partial<BoxedTabsProps<string>> = {}) {
  return shallow(dom(overrides));
}

function mountRender(overrides: Partial<BoxedTabsProps<string>> = {}) {
  return mount(dom(overrides));
}

function dom(overrides) {
  return (
    <BoxedTabs
      className="boxed-tabs"
      onSelect={jest.fn()}
      selected="a"
      tabs={[
        { key: 'a', label: 'labela' },
        { key: 'b', label: 'labelb' },
        {
          key: 'c',
          label: (
            <span>
              Complex label <strong>!!!</strong>
            </span>
          ),
        },
      ]}
      {...overrides}
    />
  );
}
