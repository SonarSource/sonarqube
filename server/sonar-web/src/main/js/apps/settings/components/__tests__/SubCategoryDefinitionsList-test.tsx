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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { mockSettingWithCategory } from '../../../../helpers/mocks/settings';
import { mockLocation } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import {
  SubCategoryDefinitionsList,
  SubCategoryDefinitionsListProps,
} from '../SubCategoryDefinitionsList';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('');
  expect(shallowRender({ subCategory: 'qg' })).toMatchSnapshot('subcategory');
});

it('should scroll if hash is defined and updated', async () => {
  window.HTMLElement.prototype.scrollIntoView = jest.fn();
  const wrapper = shallowRender({ location: mockLocation({ hash: '#qg' }) });

  await waitAndUpdate(wrapper);

  wrapper.find('h2').forEach((node) => mount(node.getElement()));

  expect(window.HTMLElement.prototype.scrollIntoView).toHaveBeenCalled();

  wrapper.setProps({ location: mockLocation({ hash: '#email' }) });

  expect(window.HTMLElement.prototype.scrollIntoView).toHaveBeenCalled();
});

function shallowRender(props: Partial<SubCategoryDefinitionsListProps> = {}) {
  return shallow<SubCategoryDefinitionsListProps>(
    <SubCategoryDefinitionsList
      category="general"
      location={mockLocation()}
      settings={[
        mockSettingWithCategory(),
        mockSettingWithCategory({
          definition: {
            key: 'qg',
            category: 'general',
            subCategory: 'qg',
            fields: [],
            options: [],
            description: 'awesome description',
          },
        }),
      ]}
      {...props}
    />
  );
}
