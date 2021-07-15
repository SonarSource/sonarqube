/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { click } from '../../../../helpers/testUtils';
import MetaDataVersion from '../MetaDataVersion';
import MetaDataVersions from '../MetaDataVersions';
import { mockMetaDataVersionInformation } from '../mocks/update-center-metadata';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should properly handle show more / show less', () => {
  const wrapper = shallowRender();
  expect(wrapper.find(MetaDataVersion).length).toBe(1);

  click(wrapper.find('.update-center-meta-data-versions-show-more'));
  expect(wrapper.find(MetaDataVersion).length).toBe(3);
});

function shallowRender(props?: Partial<MetaDataVersions['props']>) {
  return shallow<MetaDataVersions>(
    <MetaDataVersions
      versions={[
        mockMetaDataVersionInformation({ version: '3.0' }),
        mockMetaDataVersionInformation({ version: '2.0', archived: true }),
        mockMetaDataVersionInformation({ version: '1.0', archived: true }),
      ]}
      {...props}
    />
  );
}
