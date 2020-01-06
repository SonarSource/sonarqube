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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { deleteProfile } from '../../../../api/quality-profiles';
import { mockEvent, mockQualityProfile } from '../../../../helpers/testMocks';
import DeleteProfileForm from '../DeleteProfileForm';

beforeEach(() => jest.clearAllMocks());

jest.mock('../../../../api/quality-profiles', () => ({
  deleteProfile: jest.fn().mockResolvedValue({})
}));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should handle form submit correctly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleFormSubmit(mockEvent());
  await waitAndUpdate(wrapper);

  expect(deleteProfile).toHaveBeenCalled();
});

function shallowRender(props: Partial<DeleteProfileForm['props']> = {}) {
  return shallow<DeleteProfileForm>(
    <DeleteProfileForm
      onClose={jest.fn()}
      onDelete={jest.fn()}
      profile={mockQualityProfile()}
      {...props}
    />
  );
}
