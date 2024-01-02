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
import { mockComponent } from '../../../helpers/mocks/component';
import { ComponentQualifier } from '../../../types/component';
import ComponentReportActionsRenderer, {
  ComponentReportActionsRendererProps,
} from '../ComponentReportActionsRenderer';

it('should render correctly', () => {
  expect(shallowRender({ canSubscribe: false })).toMatchSnapshot('cannot subscribe');
  expect(shallowRender({ canSubscribe: true, subscribed: false })).toMatchSnapshot(
    'can subscribe, not subscribed'
  );
  expect(shallowRender({ canSubscribe: true, subscribed: true })).toMatchSnapshot(
    'can subscribe, subscribed'
  );
  expect(shallowRender({ canSubscribe: true, currentUserHasEmail: false })).toMatchSnapshot(
    'current user without email'
  );
  expect(shallowRender({ component: mockComponent() })).toMatchSnapshot('not a portfolio');
});

function shallowRender(props: Partial<ComponentReportActionsRendererProps> = {}) {
  return shallow<ComponentReportActionsRendererProps>(
    <ComponentReportActionsRenderer
      component={mockComponent({ qualifier: ComponentQualifier.Portfolio })}
      canSubscribe={true}
      subscribed={false}
      currentUserHasEmail={true}
      frequency="weekly"
      handleSubscription={jest.fn()}
      handleUnsubscription={jest.fn()}
      {...props}
    />
  );
}
