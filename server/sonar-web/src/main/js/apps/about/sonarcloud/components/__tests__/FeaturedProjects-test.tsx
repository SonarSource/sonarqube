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
import FeaturedProjects, { ProjectCard, ProjectIssues } from '../FeaturedProjects';
import { requestFeaturedProjects } from '../../utils';
import { click, waitAndUpdate } from '../../../../../helpers/testUtils';

jest.mock('../../utils', () => ({
  requestFeaturedProjects: jest.fn(() =>
    Promise.resolve([
      {
        key: 'foo',
        measures: { coverage: '20', foo: '15' },
        name: 'Foo',
        organization: { name: 'Foo', avatar: '' }
      },
      {
        key: 'bar',
        measures: { bar: '20', foo: '15' },
        name: 'Bar',
        organization: { name: 'Bar', avatar: '' }
      },
      {
        key: 'baz',
        measures: { bar: '20', foo: '15' },
        name: 'Baz',
        organization: { name: 'Baz', avatar: '' }
      },
      {
        key: 'foobar',
        measures: { bar: '20', foo: '15' },
        name: 'Foobar',
        organization: { name: 'Foobar', avatar: '' }
      }
    ])
  )
}));

const PROJECT = {
  key: 'foo',
  measures: { bar: '20', foo: '15' },
  name: 'Foo',
  organization: { name: 'Foo', avatar: '' }
};

beforeEach(() => {
  (requestFeaturedProjects as jest.Mock<any>).mockClear();
});

it('should render ProjectIssues correctly', () => {
  expect(
    shallow(
      <ProjectIssues
        measures={{ bar: '20', foo: '15' }}
        metric="foo"
        ratingMetric="bar"
        viewable={false}
      />
    )
  ).toMatchSnapshot();
});

it('should render ProjectCard correctly', () => {
  expect(shallow(<ProjectCard order={1} project={PROJECT} viewable={false} />)).toMatchSnapshot();
});

it('should render correctly', async () => {
  const wrapper = shallow(<FeaturedProjects />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should cycle through projects', async () => {
  const wrapper = shallow(<FeaturedProjects />);
  expect(wrapper.find('DeferredSpinner')).toHaveLength(1);

  await waitAndUpdate(wrapper);

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
