/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import ListFooter from '../../../../components/controls/ListFooter';
import { mockAzureProject, mockAzureRepository } from '../../../../helpers/mocks/alm-integrations';
import AzureProjectAccordion from '../AzureProjectAccordion';
import AzureProjectsList, { AzureProjectsListProps } from '../AzureProjectsList';

it('should render correctly', () => {
  expect(shallowRender({})).toMatchSnapshot('default');
  expect(shallowRender({ projects: [] })).toMatchSnapshot('empty');
});

it('should render search results correctly', () => {
  const projects = [
    mockAzureProject({ name: 'p1', description: 'p1' }),
    mockAzureProject({ name: 'p2', description: 'p2' }),
    mockAzureProject({ name: 'p3', description: 'p3' })
  ];
  const searchResults = {
    p2: [mockAzureRepository({ projectName: 'p2' })]
  };
  expect(shallowRender({ searchResults, projects })).toMatchSnapshot('default');
  expect(shallowRender({ searchResults: {}, projects })).toMatchSnapshot('empty');
});

it('should handle pagination', () => {
  const projects = new Array(21)
    .fill(1)
    .map((_, i) => mockAzureProject({ name: `project-${i}`, description: `Project #${i}` }));

  const wrapper = shallowRender({ projects });

  expect(wrapper.find(AzureProjectAccordion)).toHaveLength(10);

  wrapper.find(ListFooter).props().loadMore!();

  expect(wrapper.find(AzureProjectAccordion)).toHaveLength(20);
});

function shallowRender(overrides: Partial<AzureProjectsListProps> = {}) {
  const project = mockAzureProject();

  return shallow(
    <AzureProjectsList
      importing={false}
      loadingRepositories={{}}
      onOpenProject={jest.fn()}
      onSelectRepository={jest.fn()}
      projects={[project]}
      repositories={{ [project.name]: [] }}
      {...overrides}
    />
  );
}
