/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ProjectCardOverall from '../ProjectCardOverall';

const MEASURES = {
  alert_status: 'OK',
  reliability_rating: '1.0',
  sqale_rating: '1.0',
  new_bugs: '12'
};

const PROJECT = {
  analysisDate: '2017-01-01',
  key: 'foo',
  measures: MEASURES,
  name: 'Foo',
  organization: { key: 'org', name: 'org' },
  tags: [],
  visibility: 'public'
};

it('should display analysis date (and not leak period) when defined', () => {
  expect(
    shallow(<ProjectCardOverall project={PROJECT} />)
      .find('.project-card-dates')
      .exists()
  ).toBeTruthy();
  expect(
    shallow(<ProjectCardOverall project={{ ...PROJECT, analysisDate: undefined }} />)
      .find('.project-card-dates')
      .exists()
  ).toBeFalsy();
});

it('should not display the quality gate', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  expect(
    shallow(<ProjectCardOverall project={project} />)
      .find('ProjectCardOverallQualityGate')
      .exists()
  ).toBeFalsy();
});

it('should display tags', () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  expect(
    shallow(<ProjectCardOverall project={project} />)
      .find('TagsList')
      .exists()
  ).toBeTruthy();
});

it('should private badge', () => {
  const project = { ...PROJECT, visibility: 'private' };
  expect(
    shallow(<ProjectCardOverall project={project} />)
      .find('PrivateBadge')
      .exists()
  ).toBeTruthy();
});

it('should display the overall measures and quality gate', () => {
  expect(shallow(<ProjectCardOverall project={PROJECT} />)).toMatchSnapshot();
});
