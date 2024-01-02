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
import { mockProjectAlmBindingResponse } from '../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import {
  FirstAnalysisNextStepsNotif,
  FirstAnalysisNextStepsNotifProps,
} from '../FirstAnalysisNextStepsNotif';

it('should render correctly', () => {
  expect(shallowRender({ currentUser: mockCurrentUser() }).type()).toBeNull();
  expect(
    shallowRender({
      component: mockComponent({ qualifier: ComponentQualifier.Application }),
    }).type()
  ).toBeNull();
  expect(shallowRender({ detectedCIOnLastAnalysis: false })).toMatchSnapshot(
    'show prompt to configure CI'
  );
  expect(
    shallowRender({
      projectBinding: undefined,
    })
  ).toMatchSnapshot('show prompt to configure PR decoration, regular user');
  expect(
    shallowRender({
      component: mockComponent({ configuration: { showSettings: true } }),
      projectBinding: undefined,
    })
  ).toMatchSnapshot('show prompt to configure PR decoration, project admin');
  expect(
    shallowRender({
      projectBinding: undefined,
      detectedCIOnLastAnalysis: false,
    })
  ).toMatchSnapshot('show prompt to configure PR decoration + CI, regular user');
  expect(
    shallowRender({
      component: mockComponent({ configuration: { showSettings: true } }),
      projectBinding: undefined,
      detectedCIOnLastAnalysis: false,
    })
  ).toMatchSnapshot('show prompt to configure PR decoration + CI, project admin');
});

function shallowRender(props: Partial<FirstAnalysisNextStepsNotifProps> = {}) {
  return shallow<FirstAnalysisNextStepsNotifProps>(
    <FirstAnalysisNextStepsNotif
      component={mockComponent()}
      branchesEnabled={true}
      currentUser={mockLoggedInUser()}
      detectedCIOnLastAnalysis={true}
      projectBinding={mockProjectAlmBindingResponse()}
      {...props}
    />
  );
}
