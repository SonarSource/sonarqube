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
import { mockQualityProfile } from '../../../../helpers/testMocks';
import EvolutionDeprecated from '../EvolutionDeprecated';

it('should render correctly', () => {
  const wrapper = shallow(
    <EvolutionDeprecated
      organization="foo"
      profiles={[
        mockQualityProfile({
          key: 'qp-1',
          name: 'Quality Profile 1',
          activeDeprecatedRuleCount: 0
        }),
        mockQualityProfile({
          key: 'qp-2',
          name: 'Quality Profile 2',
          childrenCount: 1,
          activeDeprecatedRuleCount: 2
        }),
        mockQualityProfile({
          key: 'qp-3',
          name: 'Quality Profile 3',
          depth: 2,
          activeDeprecatedRuleCount: 2,
          parentKey: 'qp-2'
        }),
        mockQualityProfile({
          key: 'qp-4',
          name: 'Quality Profile 4',
          depth: 3,
          activeDeprecatedRuleCount: 3,
          parentKey: 'qp-3'
        }),
        mockQualityProfile({
          key: 'qp-5',
          name: 'Quality Profile 5',
          depth: 4,
          activeDeprecatedRuleCount: 4,
          parentKey: 'qp-4'
        })
      ]}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
