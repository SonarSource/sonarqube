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
  isScannable: false,
};

const USER_LOGGED_OUT = mockCurrentUser();
const USER_LOGGED_IN = mockLoggedInUser();

it('should not display the quality gate', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  renderProjectCard(project);
  expect(screen.getByText('projects.not_analyzed.TRK')).toBeInTheDocument();
});

it('should display tags', async () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  renderProjectCard(project);
  await expect(screen.getByText('foo')).toHaveATooltipWithContent('foo, bar');
});

it('should display private badge', () => {
  const project: Project = { ...PROJECT, visibility: Visibility.Private };
  renderProjectCard(project);
  expect(screen.getByLabelText('visibility.private')).toBeInTheDocument();
});

it('should display configure analysis button for logged in user and scan rights', () => {
  const user = mockLoggedInUser();
  renderProjectCard({ ...PROJECT, isScannable: true, analysisDate: undefined }, user);
  expect(screen.getByText('projects.configure_analysis')).toBeInTheDocument();
});

it('should not display configure analysis button for logged in user and without scan rights', () => {
  renderProjectCard({ ...PROJECT, analysisDate: undefined }, USER_LOGGED_IN);
  expect(screen.queryByText('projects.configure_analysis')).not.toBeInTheDocument();
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
    <ProjectCard currentUser={user} handleFavorite={jest.fn()} project={project} type={type} />,
  );
}
