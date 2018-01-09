/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Header, { Props } from '../Header';
import { Visibility } from '../../../app/types';
import { click } from '../../../helpers/testUtils';

const organization = { key: 'org', name: 'org', projectVisibility: Visibility.Public };

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('creates project', () => {
  const onProjectCreate = jest.fn();
  const wrapper = shallowRender({ onProjectCreate });
  click(wrapper.find('#create-project'));
  expect(onProjectCreate).toBeCalledWith();
});

it('changes default visibility', () => {
  const onVisibilityChange = jest.fn();
  const wrapper = shallowRender({ onVisibilityChange });

  click(wrapper.find('.js-change-visibility'));

  const modalWrapper = wrapper.find('ChangeVisibilityForm');
  expect(modalWrapper).toMatchSnapshot();
  modalWrapper.prop<Function>('onConfirm')(Visibility.Private);
  expect(onVisibilityChange).toBeCalledWith(Visibility.Private);

  modalWrapper.prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('ChangeVisibilityForm').exists()).toBeFalsy();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <Header
      hasProvisionPermission={true}
      onProjectCreate={jest.fn()}
      onVisibilityChange={jest.fn()}
      organization={organization}
      {...props}
    />
  );
}
