/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import { submit } from 'sonar-ui-common/helpers/testUtils';
import AlmBindingDefinitionFormRenderer, {
  AlmBindingDefinitionFormRendererProps
} from '../AlmBindingDefinitionFormRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ onCancel: jest.fn() })).toMatchSnapshot();
  expect(shallowRender({ onDelete: jest.fn() })).toMatchSnapshot();
  expect(shallowRender({ success: true })).toMatchSnapshot();
  expect(shallowRender({ loading: true })).toMatchSnapshot();
});

it('should correctly block the form submission', () => {
  const canSubmit = jest.fn(() => false);
  const wrapper = shallowRender({ canSubmit, loading: false });

  expect(canSubmit).toBeCalled();
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);

  wrapper.setProps({ canSubmit: jest.fn(), loading: true });
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);

  wrapper.setProps({ canSubmit: () => true, loading: false });
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(false);
});

it('should correctly submit the form', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });
  submit(wrapper.find('form'));
  expect(onSubmit).toBeCalled();
});

function shallowRender(props: Partial<AlmBindingDefinitionFormRendererProps> = {}) {
  return shallow(
    <AlmBindingDefinitionFormRenderer
      canSubmit={jest.fn()}
      loading={false}
      onSubmit={jest.fn()}
      success={false}
      {...props}>
      {() => null}
    </AlmBindingDefinitionFormRenderer>
  );
}
