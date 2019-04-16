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
import FeaturedProjects, { ProjectCard, ProjectIssues } from '../FeaturedProjects';
import { click } from '../../../../../helpers/testUtils';

const PROJECTS = [
  {
    key: 'sonarsource-jfrog.simple-js-php-project',
    avatarUrl: null,
    organizationKey: 'sonarsource-jfrog',
    organizationName: 'SonarSource & JFrog',
    name: 'Simple JS & PHP project',
    bugs: 0,
    codeSmells: 7,
    coverage: 9.7,
    duplications: 56.2,
    gateStatus: 'OK',
    languages: ['js', 'php'],
    maintainabilityRating: 1,
    ncloc: 324,
    reliabilityRating: 1,
    securityRating: 1,
    vulnerabilities: 0
  },
  {
    key: 'example-js',
    avatarUrl: null,
    organizationKey: 'autoscan',
    organizationName: 'AutoScan',
    name: 'example-js',
    bugs: 13,
    codeSmells: 5,
    coverage: 0,
    duplications: 0,
    gateStatus: 'OK',
    languages: ['go', 'js', 'php', 'py'],
    maintainabilityRating: 1,
    ncloc: 80,
    reliabilityRating: 1,
    securityRating: 1,
    vulnerabilities: 0
  },
  {
    key: 'example-js-2',
    avatarUrl: null,
    organizationKey: 'autoscan',
    organizationName: 'AutoScan',
    name: 'example-js',
    bugs: 13,
    codeSmells: 5,
    coverage: 0,
    duplications: 0,
    gateStatus: 'OK',
    languages: ['go', 'js', 'php', 'py'],
    maintainabilityRating: 1,
    ncloc: 80,
    reliabilityRating: 1,
    securityRating: 1,
    vulnerabilities: 0
  },
  {
    key: 'example-js-3',
    avatarUrl: null,
    organizationKey: 'autoscan',
    organizationName: 'AutoScan',
    name: 'example-js',
    bugs: 13,
    codeSmells: 5,
    coverage: 0,
    duplications: 0,
    gateStatus: 'OK',
    languages: ['go', 'js', 'php', 'py'],
    maintainabilityRating: 1,
    ncloc: 80,
    reliabilityRating: 1,
    securityRating: 1,
    vulnerabilities: 0
  },
  {
    key: 'example-js-4',
    avatarUrl: null,
    organizationKey: 'autoscan',
    organizationName: 'AutoScan',
    name: 'example-js',
    bugs: 13,
    codeSmells: 5,
    coverage: 0,
    duplications: 0,
    gateStatus: 'OK',
    languages: ['go', 'js', 'php', 'py'],
    maintainabilityRating: 1,
    ncloc: 80,
    reliabilityRating: 1,
    securityRating: 1,
    vulnerabilities: 0
  }
];

it('should render ProjectIssues correctly', () => {
  expect(
    shallow(<ProjectIssues metric={5} metricKey="foo" ratingMetric={20} viewable={true} />)
  ).toMatchSnapshot();
  expect(
    shallow(<ProjectIssues metric={15000} metricKey="foo" ratingMetric={20} viewable={true} />)
  ).toMatchSnapshot();
});

it('should render ProjectCard correctly', () => {
  expect(
    shallow(<ProjectCard order={1} project={PROJECTS[0]} viewable={true} />)
  ).toMatchSnapshot();
});

it('should render ProjectCard correctly when there is no coverage', () => {
  expect(
    shallow(
      <ProjectCard order={1} project={{ ...PROJECTS[0], coverage: undefined }} viewable={true} />
    )
      .find('li')
      .first()
  ).toMatchSnapshot();
});

it('should render correctly', () => {
  const wrapper = shallow(<FeaturedProjects projects={PROJECTS} />);
  expect(wrapper).toMatchSnapshot();
});

it('should cycle through projects', () => {
  const wrapper = shallow<FeaturedProjects>(<FeaturedProjects projects={PROJECTS} />);
  expect(wrapper.state().slides.map((slide: any) => slide.order)).toEqual([0, 1, 2, 3]);

  click(wrapper.find('.js-next'));
  expect(wrapper.state().slides.map((slide: any) => slide.order)).toEqual([3, 0, 1, 2]);

  click(wrapper.find('.js-next'));
  click(wrapper.find('.js-next'));
  expect(wrapper.state().slides.map((slide: any) => slide.order)).toEqual([1, 2, 3, 0]);

  click(wrapper.find('.js-prev'));
  click(wrapper.find('.js-prev'));
  expect(wrapper.state().slides.map((slide: any) => slide.order)).toEqual([3, 0, 1, 2]);
});
