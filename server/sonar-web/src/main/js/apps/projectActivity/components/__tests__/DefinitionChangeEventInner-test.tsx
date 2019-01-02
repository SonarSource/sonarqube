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
import * as React from 'react';
import { shallow } from 'enzyme';
import { DefinitionChangeEventInner, DefinitionChangeEvent } from '../DefinitionChangeEventInner';
import { click } from '../../../../helpers/testUtils';

it('should render', () => {
  const event: DefinitionChangeEvent = {
    category: 'DEFINITION_CHANGE',
    key: 'foo1234',
    name: '',
    definitionChange: {
      projects: [
        { changeType: 'ADDED', key: 'foo', name: 'Foo', branch: 'master' },
        { changeType: 'REMOVED', key: 'bar', name: 'Bar', branch: 'master' }
      ]
    }
  };
  const wrapper = shallow(<DefinitionChangeEventInner branchLike={undefined} event={event} />);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.project-activity-event-inner-more-link'));
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should render for a branch', () => {
  const branch: T.LongLivingBranch = { name: 'feature-x', isMain: false, type: 'LONG' };
  const event: DefinitionChangeEvent = {
    category: 'DEFINITION_CHANGE',
    key: 'foo1234',
    name: '',
    definitionChange: {
      projects: [
        { changeType: 'ADDED', key: 'foo', name: 'Foo', branch: 'feature-x' },
        {
          changeType: 'BRANCH_CHANGED',
          key: 'bar',
          name: 'Bar',
          oldBranch: 'master',
          newBranch: 'feature-y'
        }
      ]
    }
  };
  const wrapper = shallow(<DefinitionChangeEventInner branchLike={branch} event={event} />);
  click(wrapper.find('.project-activity-event-inner-more-link'));
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});
