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
import { createApplication } from '../../../../api/application';
import SimpleModal from '../../../../components/controls/SimpleModal';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier, Visibility } from '../../../../types/component';
import CreateApplicationForm from '../CreateApplicationForm';

jest.mock('../../../../api/application', () => ({
  createApplication: jest.fn().mockResolvedValue({ application: { key: 'foo' } }),
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('default');
  expect(wrapper.find(SimpleModal).dive()).toMatchSnapshot('form');
});

it('should correctly create application on form submit', async () => {
  const onCreate = jest.fn();
  const wrapper = shallowRender({ onCreate });
  const instance = wrapper.instance();

  instance.handleDescriptionChange(mockEvent({ currentTarget: { value: 'description' } }));
  instance.handleKeyChange(mockEvent({ currentTarget: { value: 'key' } }));
  instance.handleNameChange(mockEvent({ currentTarget: { value: 'name' } }));
  instance.handleVisibilityChange(Visibility.Private);

  wrapper.find(SimpleModal).props().onSubmit();
  expect(createApplication).toHaveBeenCalledWith('name', 'description', 'key', Visibility.Private);
  await waitAndUpdate(wrapper);

  expect(onCreate).toHaveBeenCalledWith(
    expect.objectContaining({
      key: 'foo',
      qualifier: ComponentQualifier.Application,
    })
  );

  // Can call the WS without any key.
  instance.handleKeyChange(mockEvent({ currentTarget: { value: '' } }));
  instance.handleFormSubmit();
  expect(createApplication).toHaveBeenCalledWith(
    'name',
    'description',
    undefined,
    Visibility.Private
  );
});

function shallowRender(props?: Partial<CreateApplicationForm['props']>) {
  return shallow<CreateApplicationForm>(
    <CreateApplicationForm onClose={jest.fn()} onCreate={jest.fn()} {...props} />
  );
}
