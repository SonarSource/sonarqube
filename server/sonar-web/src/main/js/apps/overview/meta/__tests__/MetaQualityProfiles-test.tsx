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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { searchRules } from '../../../../api/rules';
import { mockLanguage, mockQualityProfile } from '../../../../helpers/testMocks';
import { MetaQualityProfiles } from '../MetaQualityProfiles';

jest.mock('../../../../api/rules', () => {
  return {
    searchRules: jest.fn().mockResolvedValue({
      total: 10
    })
  };
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.overview-deprecated-rules').exists()).toBe(true);
  expect(wrapper.find('.overview-deleted-profile').exists()).toBe(true);
  expect(searchRules).toBeCalled();
});

function shallowRender(props: Partial<MetaQualityProfiles['props']> = {}) {
  return shallow(
    <MetaQualityProfiles
      languages={{ css: mockLanguage() }}
      profiles={[
        { ...mockQualityProfile({ key: 'js' }), deleted: true },
        {
          ...mockQualityProfile({ key: 'css', language: 'css', languageName: 'CSS' }),
          deleted: false
        }
      ]}
      {...props}
    />
  );
}
