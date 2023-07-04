/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { screen } from '@testing-library/react';
import React from 'react';
import { mockCurrentUser, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier, Visibility } from '../../../../../types/component';
import { CurrentUser } from '../../../../../types/users';
import { Project } from '../../../types';
import ProjectCard from '../ProjectCard';

const MEASURES = {
  alert_status: 'OK',
  reliability_rating: '1.0',
  sqale_rating: '1.0',
  new_bugs: '12',
};

const PROJECT: Project = {
  analysisDate: '2017-01-01',
  key: 'foo',
  measures: MEASURES,
  name: 'Foo',
  qualifier: ComponentQualifier.Project,
  tags: [],
  visibility: Visibility.Public,
};

const USER_LOGGED_OUT = mockCurrentUser();
const USER_LOGGED_IN = mockLoggedInUser();

it('should display correclty when project need issue synch and not setup', () => {
  renderProjectCard({ ...PROJECT, needIssueSync: true });
  expect(screen.getByLabelText('overview.quality_gate_x.OK')).toBeInTheDocument();
  expect(screen.getByText('overview.project.main_branch_empty')).toBeInTheDocument();
});

it('should not display the quality gate', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  renderProjectCard(project);
  expect(screen.getByText('projects.not_analyzed.TRK')).toBeInTheDocument();
});

it('should display tags', () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  renderProjectCard(project);
  expect(screen.getByTitle('foo, bar')).toBeInTheDocument();
});

it('should display private badge', () => {
  const project: Project = { ...PROJECT, visibility: Visibility.Private };
  renderProjectCard(project);
  expect(screen.getByLabelText('visibility.private')).toBeInTheDocument();
});

it('should display configure analysis button for logged in user', () => {
  renderProjectCard({ ...PROJECT, analysisDate: undefined }, USER_LOGGED_IN);
  expect(screen.getByText('projects.configure_analysis')).toBeInTheDocument();
});

it('should display applications', () => {
  renderProjectCard({ ...PROJECT, qualifier: ComponentQualifier.Application });
  expect(screen.getByLabelText('qualifier.APP')).toBeInTheDocument();
});

it('should display 3 aplication', () => {
  renderProjectCard({
    ...PROJECT,
    qualifier: ComponentQualifier.Application,
    measures: { ...MEASURES, projects: '3' },
  });
  expect(screen.getByText(/x_projects_.3/)).toBeInTheDocument();
});

function renderProjectCard(project: Project, user: CurrentUser = USER_LOGGED_OUT, type?: string) {
  renderComponent(
    <ProjectCard currentUser={user} handleFavorite={jest.fn()} project={project} type={type} />
  );
}
