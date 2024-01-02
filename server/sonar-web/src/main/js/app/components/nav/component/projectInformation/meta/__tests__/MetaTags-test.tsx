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
import { shallow } from 'enzyme';
import * as React from 'react';
import { setApplicationTags, setProjectTags } from '../../../../../../../api/components';
import { mockComponent } from '../../../../../../../helpers/mocks/component';
import { ComponentQualifier } from '../../../../../../../types/component';
import MetaTags from '../MetaTags';

jest.mock('../../../../../../../api/components', () => ({
  setApplicationTags: jest.fn().mockResolvedValue(true),
  setProjectTags: jest.fn().mockResolvedValue(true),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render without tags and admin rights', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render with tags and admin rights', () => {
  const component = mockComponent({
    key: 'my-second-project',
    tags: ['foo', 'bar'],
    configuration: {
      showSettings: true,
    },
    name: 'MySecondProject',
  });

  expect(shallowRender({ component })).toMatchSnapshot();
});

it('should set tags for a project', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleSetProjectTags(['tag1', 'tag2']);

  expect(setProjectTags).toHaveBeenCalled();
  expect(setApplicationTags).not.toHaveBeenCalled();
});

it('should set tags for an app', () => {
  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Application }),
  });

  wrapper.instance().handleSetProjectTags(['tag1', 'tag2']);

  expect(setProjectTags).not.toHaveBeenCalled();
  expect(setApplicationTags).toHaveBeenCalled();
});

function shallowRender(overrides: Partial<MetaTags['props']> = {}) {
  const component = mockComponent({
    configuration: {
      showSettings: false,
    },
  });

  return shallow<MetaTags>(
    <MetaTags component={component} onComponentChange={jest.fn()} {...overrides} />
  );
}
