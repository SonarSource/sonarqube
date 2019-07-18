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
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { Project } from '../../types';
import ProjectCardLeak from '../ProjectCardLeak';

jest.mock(
  'date-fns/difference_in_milliseconds',
  () => () => 1000 * 60 * 60 * 24 * 30 * 8 // ~ 8 months
);

const MEASURES = {
  alert_status: 'OK',
  reliability_rating: '1.0',
  sqale_rating: '1.0',
  new_bugs: '12'
};

const PROJECT: Project = {
  analysisDate: '2017-01-01',
  leakPeriodDate: '2016-12-01',
  key: 'foo',
  measures: MEASURES,
  name: 'Foo',
  organization: { key: 'org', name: 'org' },
  tags: [],
  visibility: 'public'
};

const USER_LOGGED_OUT = mockCurrentUser();
const USER_LOGGED_IN = mockLoggedInUser();

it('should display analysis date and leak start date', () => {
  const card = shallowRender(PROJECT);
  expect(card.find('.project-card-dates').exists()).toBeTruthy();
  expect(card.find('.project-card-dates').find('.project-card-leak-date')).toHaveLength(1);
  expect(card.find('.project-card-dates').find('DateTimeFormatter')).toHaveLength(1);
});

it('should not display analysis date or leak start date', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  const card = shallowRender(project);
  expect(card.find('.project-card-dates').exists()).toBeFalsy();
});

it('should display tags', () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  expect(
    shallowRender(project)
      .find('TagsList')
      .exists()
  ).toBeTruthy();
});

it('should display private badge', () => {
  const project: Project = { ...PROJECT, visibility: 'private' };
  expect(
    shallowRender(project)
      .find('Connect(PrivacyBadge)')
      .exists()
  ).toBeTruthy();
});

it('should display the leak measures and quality gate', () => {
  expect(shallowRender(PROJECT)).toMatchSnapshot();
});

it('should display not analyzed yet', () => {
  expect(shallowRender({ ...PROJECT, analysisDate: undefined })).toMatchSnapshot();
});

it('should display configure analysis button for logged in user', () => {
  expect(shallowRender({ ...PROJECT, analysisDate: undefined }, USER_LOGGED_IN)).toMatchSnapshot();
});

function shallowRender(project: Project, user: T.CurrentUser = USER_LOGGED_OUT) {
  return shallow(
    <ProjectCardLeak
      currentUser={user}
      handleFavorite={jest.fn()}
      height={100}
      organization={undefined}
      project={project}
    />
  );
}
