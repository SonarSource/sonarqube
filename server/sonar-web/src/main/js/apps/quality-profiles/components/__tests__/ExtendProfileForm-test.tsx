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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { changeProfileParent, createQualityProfile } from '../../../../api/quality-profiles';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import ExtendProfileForm from '../ExtendProfileForm';

jest.mock('../../../../api/quality-profiles', () => ({
  createQualityProfile: jest.fn().mockResolvedValue({ profile: { key: 'new-profile' } }),
  changeProfileParent: jest.fn().mockResolvedValue(true)
}));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should correctly create a new profile and extend the existing one', async () => {
  const profile = mockQualityProfile();
  const organization = 'org';
  const name = 'New name';
  const wrapper = shallowRender({ organization, profile });

  click(wrapper.find('SubmitButton'));
  expect(createQualityProfile).not.toHaveBeenCalled();
  expect(changeProfileParent).not.toHaveBeenCalled();

  wrapper.setState({ name }).update();
  click(wrapper.find('SubmitButton'));
  await waitAndUpdate(wrapper);

  const data = new FormData();
  data.append('language', profile.language);
  data.append('name', name);
  data.append('organization', organization);
  expect(createQualityProfile).toHaveBeenCalledWith(data);
  expect(changeProfileParent).toHaveBeenCalledWith('new-profile', profile.key);
});

function shallowRender(props: Partial<ExtendProfileForm['props']> = {}) {
  return shallow(
    <ExtendProfileForm
      onClose={jest.fn()}
      onExtend={jest.fn()}
      organization="foo"
      profile={mockQualityProfile()}
      {...props}
    />
  );
}
