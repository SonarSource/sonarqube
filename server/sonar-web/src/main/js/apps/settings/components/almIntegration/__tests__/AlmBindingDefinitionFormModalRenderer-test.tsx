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
import { mockEvent } from '../../../../../helpers/testMocks';
import AlmBindingDefinitionFormModalRenderer, {
  AlmBindingDefinitionFormModalProps
} from '../AlmBindingDefinitionFormModalRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ help: <span>Help me</span> })).toMatchSnapshot('with help');
  expect(shallowRender({ isSecondInstance: true })).toMatchSnapshot('second instance');
});

it('should submit properly', () => {
  const onSubmit = jest.fn().mockResolvedValue({});
  const wrapper = shallowRender({ onSubmit });

  const event: React.SyntheticEvent<HTMLFormElement> = mockEvent({ preventDefault: jest.fn() });

  wrapper.find('form').simulate('submit', event);

  expect(event.preventDefault).toBeCalled();
  expect(onSubmit).toBeCalled();
});

function shallowRender(props: Partial<AlmBindingDefinitionFormModalProps> = {}) {
  return shallow(
    <AlmBindingDefinitionFormModalRenderer
      action="create"
      canSubmit={jest.fn()}
      isSecondInstance={false}
      onCancel={jest.fn()}
      onSubmit={jest.fn()}
      {...props}>
      {() => null}
    </AlmBindingDefinitionFormModalRenderer>
  );
}
