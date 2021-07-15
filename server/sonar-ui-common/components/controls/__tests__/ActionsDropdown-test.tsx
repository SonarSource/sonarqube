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
/* eslint-disable sonarjs/no-duplicate-string */
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { click } from '../../../helpers/testUtils';
import { PopupPlacement } from '../../ui/popups';
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem,
  ActionsDropdownProps,
} from '../ActionsDropdown';

describe('ActionsDropdown', () => {
  it('should render correctly', () => {
    expect(shallowRender()).toMatchSnapshot();
    expect(shallowRender({ small: false })).toMatchSnapshot();
  });

  function shallowRender(props: Partial<ActionsDropdownProps> = {}) {
    return shallow(
      <ActionsDropdown
        className="foo"
        onOpen={jest.fn()}
        overlayPlacement={PopupPlacement.Bottom}
        small={true}
        toggleClassName="bar"
        {...props}>
        <span>Hello world</span>
      </ActionsDropdown>
    );
  }
});

describe('ActionsDropdownItem', () => {
  it('should render correctly', () => {
    expect(shallowRender()).toMatchSnapshot();
    expect(shallowRender({ destructive: true, id: 'baz', to: 'path/name' })).toMatchSnapshot();
    expect(shallowRender({ download: 'foo/bar', to: 'path/name' })).toMatchSnapshot();
  });

  it('should trigger click', () => {
    const onClick = jest.fn();
    const wrapper = shallowRender({ onClick });
    click(wrapper.find('a'));
    expect(onClick).toBeCalled();
  });

  it('should render correctly copy item', () => {
    const wrapper = mountRender({ copyValue: 'my content to copy to clipboard' });
    expect(wrapper).toMatchSnapshot();
  });

  function shallowRender(props: Partial<ActionsDropdownItem['props']> = {}) {
    return shallow(renderContent(props));
  }

  function mountRender(props: Partial<ActionsDropdownItem['props']> = {}) {
    return mount(renderContent(props));
  }

  function renderContent(props: Partial<ActionsDropdownItem['props']> = {}) {
    return (
      <ActionsDropdownItem className="foo" {...props}>
        <span>Hello world</span>
      </ActionsDropdownItem>
    );
  }
});

describe('ActionsDropdownDivider', () => {
  it('should render correctly', () => {
    expect(shallow(<ActionsDropdownDivider />)).toMatchSnapshot();
  });
});
