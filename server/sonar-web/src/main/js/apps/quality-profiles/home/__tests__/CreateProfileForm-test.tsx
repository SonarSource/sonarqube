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
import { changeProfileParent, createQualityProfile } from '../../../../api/quality-profiles';
import { mockLocation, mockQualityProfile } from '../../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import CreateProfileForm from '../CreateProfileForm';

beforeEach(() => jest.clearAllMocks());

jest.mock('../../../../api/quality-profiles', () => ({
  changeProfileParent: jest.fn().mockResolvedValue({}),
  createQualityProfile: jest.fn().mockResolvedValue({}),
  getImporters: jest.fn().mockResolvedValue([
    {
      key: 'key_importer',
      languages: ['lang1_importer', 'lang2_importer', 'js'],
      name: 'name_importer'
    }
  ])
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot('default');
  expect(
    wrapper.setProps({ location: mockLocation({ query: { language: 'js' } }) })
  ).toMatchSnapshot('with query filter');
});

it('should handle form submit correctly', async () => {
  const onCreate = jest.fn();

  const wrapper = shallowRender({ onCreate });
  wrapper.instance().handleParentChange({ value: 'key' });
  wrapper.instance().handleFormSubmit(mockEvent({ currentTarget: undefined }));
  await waitAndUpdate(wrapper);

  expect(createQualityProfile).toHaveBeenCalled();
  expect(changeProfileParent).toHaveBeenCalled();
  expect(onCreate).toHaveBeenCalled();
});

it('should handle form submit without parent correctly', async () => {
  const onCreate = jest.fn();

  const wrapper = shallowRender({ onCreate });
  wrapper.instance().handleFormSubmit(mockEvent({ currentTarget: undefined }));
  await waitAndUpdate(wrapper);

  expect(createQualityProfile).toHaveBeenCalled();
  expect(changeProfileParent).not.toHaveBeenCalled();
  expect(onCreate).toHaveBeenCalled();
});

function shallowRender(props?: Partial<CreateProfileForm['props']>) {
  return shallow<CreateProfileForm>(
    <CreateProfileForm
      languages={[
        { key: 'js', name: 'JavaScript' },
        { key: 'css', name: 'CSS' }
      ]}
      location={mockLocation()}
      onClose={jest.fn()}
      onCreate={jest.fn()}
      profiles={[mockQualityProfile(), mockQualityProfile({ language: 'css' })]}
      {...props}
    />
  );
}
