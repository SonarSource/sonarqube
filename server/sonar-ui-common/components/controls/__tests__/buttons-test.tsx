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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click, mockEvent } from '../../../helpers/testUtils';
import { Button, ButtonIcon, ButtonIconProps } from '../buttons';

describe('Button', () => {
  it('should render correctly', () => {
    const onClick = jest.fn();
    const preventDefault = jest.fn();
    const stopPropagation = jest.fn();
    const wrapper = shallowRender({ onClick });
    expect(wrapper).toMatchSnapshot();
    click(wrapper.find('button'), mockEvent({ preventDefault, stopPropagation }));
    expect(onClick).toBeCalled();
    expect(preventDefault).toBeCalled();
    expect(stopPropagation).not.toBeCalled();
  });

  it('should not stop propagation, but prevent default of the click event', () => {
    const preventDefault = jest.fn();
    const stopPropagation = jest.fn();
    const wrapper = shallowRender({ preventDefault: false, stopPropagation: true });
    click(wrapper.find('button'), mockEvent({ preventDefault, stopPropagation }));
    expect(preventDefault).not.toBeCalled();
    expect(stopPropagation).toBeCalled();
  });

  it('should disable buttons with a class', () => {
    const preventDefault = jest.fn();
    const onClick = jest.fn();
    const button = shallowRender({ disabled: true, onClick, preventDefault: false }).find('button');
    expect(button.props().disabled).toBeUndefined();
    expect(button.props().className).toContain('disabled');
    expect(button.props()['aria-disabled']).toBe(true);
    click(button, mockEvent({ preventDefault }));
    expect(onClick).not.toBeCalled();
    expect(preventDefault).toBeCalled();
  });

  function shallowRender(props: Partial<Button['props']> = {}) {
    return shallow<Button>(<Button {...props}>My button</Button>);
  }
});

describe('ButtonIcon', () => {
  it('should render correctly', () => {
    const wrapper = shallowRender();
    expect(wrapper).toMatchSnapshot();
  });

  function shallowRender(props: Partial<ButtonIconProps> = {}) {
    return shallow(
      <ButtonIcon tooltip="my tooltip" tooltipProps={{ visible: true }} {...props}>
        <i />
      </ButtonIcon>
    ).dive();
  }
});
