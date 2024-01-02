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
import { getValues } from '../../../../api/settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockDefinition, mockSettingValue } from '../../../../helpers/mocks/settings';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import CategoryDefinitionsList from '../CategoryDefinitionsList';

jest.mock('../../../../api/settings', () => ({
  getValues: jest.fn().mockResolvedValue([]),
}));

it('should load settings values', async () => {
  const settings = [mockSettingValue({ key: 'yes' }), mockSettingValue({ key: 'yesagain' })];
  (getValues as jest.Mock).mockResolvedValueOnce(settings);

  const definitions = [
    mockDefinition({ category: 'general', key: 'yes' }),
    mockDefinition({ category: 'other', key: 'nope' }),
    mockDefinition({ category: 'general', key: 'yesagain' }),
  ];

  const wrapper = shallowRender({
    definitions,
  });

  await waitAndUpdate(wrapper);

  expect(getValues).toHaveBeenCalledWith({ keys: ['yes', 'yesagain'], component: undefined });

  expect(wrapper.state().settings).toEqual([
    { definition: definitions[0], settingValue: settings[0] },
    { definition: definitions[2], settingValue: settings[1] },
  ]);
});

it('should reload on category change', async () => {
  const definitions = [
    mockDefinition({ category: 'general', key: 'yes' }),
    mockDefinition({ category: 'other', key: 'nope' }),
    mockDefinition({ category: 'general', key: 'yesagain' }),
  ];
  const wrapper = shallowRender({ component: mockComponent({ key: 'comp-key' }), definitions });

  await waitAndUpdate(wrapper);

  expect(getValues).toHaveBeenCalledWith({ keys: ['yes', 'yesagain'], component: 'comp-key' });

  wrapper.setProps({ category: 'other' });

  await waitAndUpdate(wrapper);

  expect(getValues).toHaveBeenCalledWith({ keys: ['nope'], component: 'comp-key' });
});

function shallowRender(props: Partial<CategoryDefinitionsList['props']> = {}) {
  return shallow<CategoryDefinitionsList>(
    <CategoryDefinitionsList category="general" definitions={[]} {...props} />
  );
}
