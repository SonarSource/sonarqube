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
/* eslint-disable sonarjs/no-duplicate-string */
import { shallow } from 'enzyme';
import * as React from 'react';
import { click } from 'sonar-ui-common/helpers/testUtils';
import {
  mockBitbucketProject,
  mockBitbucketRepository
} from '../../../../helpers/mocks/alm-integrations';
import BitbucketProjectAccordion from '../BitbucketProjectAccordion';
import BitbucketRepositories, { BitbucketRepositoriesProps } from '../BitbucketRepositories';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ projectRepositories: {} })).toMatchSnapshot('no repos');
  expect(shallowRender({ selectedRepository: mockBitbucketRepository() })).toMatchSnapshot(
    'selected repo'
  );
});

it('should correctly handle opening/closing accordions', () => {
  const wrapper = shallowRender();
  click(wrapper.find(BitbucketProjectAccordion).at(1));
  expect(wrapper).toMatchSnapshot('2nd opened');
});

function shallowRender(props: Partial<BitbucketRepositoriesProps> = {}) {
  return shallow<BitbucketRepositoriesProps>(
    <BitbucketRepositories
      disableRepositories={false}
      onSelectRepository={jest.fn()}
      projectRepositories={{
        project: {
          allShown: true,
          repositories: [
            mockBitbucketRepository(),
            mockBitbucketRepository({ id: 2, slug: 'bar', name: 'Bar', sqProjectKey: 'bar' })
          ]
        }
      }}
      projects={[
        mockBitbucketProject(),
        mockBitbucketProject({ id: 2, key: 'project2', name: 'Project 2' })
      ]}
      {...props}
    />
  );
}
