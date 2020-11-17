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

import { shallow } from 'enzyme';
import * as React from 'react';
import BoxedGroupAccordion from 'sonar-ui-common/components/controls/BoxedGroupAccordion';
import { mockAzureProject, mockAzureRepository } from '../../../../helpers/mocks/alm-integrations';
import AzureProjectAccordion, { AzureProjectAccordionProps } from '../AzureProjectAccordion';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ startsOpen: false })).toMatchSnapshot('closed');
  expect(shallowRender({ repositories: [mockAzureRepository()] })).toMatchSnapshot(
    'with a repository'
  );
  expect(shallowRender({ importing: true, repositories: [mockAzureRepository()] })).toMatchSnapshot(
    'importing'
  );
});

it('should open when clicked', () => {
  const onOpen = jest.fn();

  const wrapper = shallowRender({
    onOpen,
    repositories: [mockAzureRepository()],
    startsOpen: false
  });
  expect(
    wrapper
      .find(BoxedGroupAccordion)
      .children()
      .exists()
  ).toBe(false);

  wrapper
    .find(BoxedGroupAccordion)
    .props()
    .onClick();

  expect(onOpen).toBeCalled();

  expect(
    wrapper
      .find(BoxedGroupAccordion)
      .children()
      .exists()
  ).toBe(true);
});

it('should close when clicked', () => {
  const onOpen = jest.fn();

  const wrapper = shallowRender({
    onOpen,
    repositories: [mockAzureRepository()]
  });

  expect(
    wrapper
      .find(BoxedGroupAccordion)
      .children()
      .exists()
  ).toBe(true);

  wrapper
    .find(BoxedGroupAccordion)
    .props()
    .onClick();

  expect(onOpen).not.toBeCalled();

  expect(
    wrapper
      .find(BoxedGroupAccordion)
      .children()
      .exists()
  ).toBe(false);
});

function shallowRender(overrides: Partial<AzureProjectAccordionProps> = {}) {
  return shallow(
    <AzureProjectAccordion
      importing={false}
      loading={false}
      onSelectRepository={jest.fn()}
      onOpen={jest.fn()}
      project={mockAzureProject()}
      startsOpen={true}
      {...overrides}
    />
  );
}
