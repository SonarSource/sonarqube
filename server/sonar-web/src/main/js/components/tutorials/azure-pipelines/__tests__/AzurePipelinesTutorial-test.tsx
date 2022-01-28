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
import { Button } from '../../../../components/controls/buttons';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { click } from '../../../../helpers/testUtils';
import Step from '../../components/Step';
import AzurePipelinesTutorial, { AzurePipelinesTutorialProps } from '../AzurePipelinesTutorial';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  wrapper.find(Step).forEach(step => {
    expect(step.dive()).toMatchSnapshot();
  });
});

it('should display the next step when one is finished', () => {
  const wrapper = shallowRender();
  expect(
    wrapper
      .find(Step)
      .filterWhere(elt => elt.props().open === true)
      .props().stepNumber
  ).toBe(1);

  click(
    wrapper
      .find(Step)
      .first()
      .dive()
      .find(Button)
  );

  expect(
    wrapper
      .find(Step)
      .filterWhere(elt => elt.props().open === true)
      .props().stepNumber
  ).toBe(2);
});

it('should open a step when user click on it', () => {
  const wrapper = shallowRender();
  expect(
    wrapper
      .find(Step)
      .filterWhere(elt => elt.props().open === true)
      .props().stepNumber
  ).toBe(1);

  (
    wrapper
      .find(Step)
      .filterWhere(elt => elt.props().stepNumber === 3)
      .props().onOpen ?? (() => {})
  )();

  expect(
    wrapper
      .find(Step)
      .filterWhere(elt => elt.props().open === true)
      .props().stepNumber
  ).toBe(3);
});

function shallowRender(props: Partial<AzurePipelinesTutorialProps> = {}) {
  return shallow<AzurePipelinesTutorialProps>(
    <AzurePipelinesTutorial
      baseUrl="http://localhost:9000"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      willRefreshAutomatically={true}
      {...props}
    />
  );
}
