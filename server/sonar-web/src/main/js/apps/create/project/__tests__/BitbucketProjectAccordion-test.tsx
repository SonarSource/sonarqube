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
import Radio from 'sonar-ui-common/components/controls/Radio';
import {
  mockBitbucketProject,
  mockBitbucketRepository
} from '../../../../helpers/mocks/alm-integrations';
import BitbucketProjectAccordion, {
  BitbucketProjectAccordionProps
} from '../BitbucketProjectAccordion';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ disableRepositories: true })).toMatchSnapshot('disable options');
  expect(shallowRender({ open: false })).toMatchSnapshot('closed');
  expect(shallowRender({ onClick: undefined })).toMatchSnapshot('no click handler');
  expect(shallowRender({ repositories: [] })).toMatchSnapshot('no repos');
  expect(shallowRender({ selectedRepository: mockBitbucketRepository() })).toMatchSnapshot(
    'selected repo'
  );
  expect(shallowRender({ showingAllRepositories: false })).toMatchSnapshot('not showing all repos');
});

it('should correctly handle selecting repos', () => {
  const onSelectRepository = jest.fn();
  const repo = mockBitbucketRepository();
  const wrapper = shallowRender({
    onSelectRepository,
    repositories: [repo]
  });

  wrapper
    .find(Radio)
    .at(0)
    .props()
    .onCheck('');
  expect(onSelectRepository).toBeCalledWith(repo);
});

function shallowRender(props: Partial<BitbucketProjectAccordionProps> = {}) {
  return shallow<BitbucketProjectAccordionProps>(
    <BitbucketProjectAccordion
      disableRepositories={false}
      onClick={jest.fn()}
      onSelectRepository={jest.fn()}
      open={true}
      project={mockBitbucketProject()}
      repositories={[
        mockBitbucketRepository(),
        mockBitbucketRepository({ id: 2, slug: 'bar', name: 'Bar', sqProjectKey: 'bar' })
      ]}
      showingAllRepositories={true}
      {...props}
    />
  );
}
