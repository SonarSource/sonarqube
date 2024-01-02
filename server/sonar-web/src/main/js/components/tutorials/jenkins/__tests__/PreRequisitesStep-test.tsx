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
import { AlmKeys } from '../../../../types/alm-settings';
import { renderStepContent } from '../../test-utils';
import PreRequisitesStep, { PreRequisitesStepProps } from '../PreRequisitesStep';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('Step wrapper');
  expect(renderStepContent(wrapper)).toMatchSnapshot('content');

  expect(renderStepContent(shallowRender({ branchesEnabled: false }))).toMatchSnapshot(
    'content for branches disabled'
  );
  expect(
    renderStepContent(shallowRender({ alm: AlmKeys.GitLab, branchesEnabled: false }))
  ).toMatchSnapshot('content for branches disabled, gitlab');
});

function shallowRender(props: Partial<PreRequisitesStepProps> = {}) {
  return shallow<PreRequisitesStepProps>(
    <PreRequisitesStep
      alm={AlmKeys.BitbucketServer}
      branchesEnabled={true}
      finished={false}
      onDone={jest.fn()}
      onOpen={jest.fn()}
      open={false}
      {...props}
    />
  );
}
