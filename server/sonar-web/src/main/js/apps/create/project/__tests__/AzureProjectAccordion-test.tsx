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
import BoxedGroupAccordion from '../../../../components/controls/BoxedGroupAccordion';
import Radio from '../../../../components/controls/Radio';
import { mockAzureProject, mockAzureRepository } from '../../../../helpers/mocks/alm-integrations';
import { mockEvent } from '../../../../helpers/testUtils';
import AzureProjectAccordion, { AzureProjectAccordionProps } from '../AzureProjectAccordion';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ startsOpen: false })).toMatchSnapshot('closed');
  expect(
    shallowRender({
      repositories: [
        mockAzureRepository(),
        mockAzureRepository({ sqProjectKey: 'sq-key', sqProjectName: 'SQ Name' })
      ]
    })
  ).toMatchSnapshot('with repositories');
  expect(shallowRender({ importing: true, repositories: [mockAzureRepository()] })).toMatchSnapshot(
    'importing'
  );
  expect(
    shallowRender({
      repositories: [
        mockAzureRepository({ name: 'this repo is the best' }),
        mockAzureRepository({
          name: 'This is a repo with class',
          sqProjectKey: 'sq-key',
          sqProjectName: 'SQ Name'
        })
      ],
      searchQuery: 'repo'
    })
  ).toMatchSnapshot('search results');
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

it('should trigger selection when repo is clicked', () => {
  const onSelectRepository = jest.fn();
  const repo = mockAzureRepository();
  const wrapper = shallowRender({ onSelectRepository, repositories: [repo] });

  wrapper
    .find(Radio)
    .props()
    .onCheck(mockEvent());

  expect(onSelectRepository).toBeCalledWith(repo);
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
