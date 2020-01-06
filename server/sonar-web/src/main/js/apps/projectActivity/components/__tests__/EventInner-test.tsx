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
import { mockAnalysisEvent } from '../../../../helpers/testMocks';
import { BranchLike } from '../../../../types/branch-like';
import EventInner, { EventInnerProps } from '../EventInner';

jest.mock('../../../../app/components/ComponentContext', () => {
  const { mockBranch } = jest.requireActual('../../../../helpers/mocks/branch-like');
  return {
    ComponentContext: {
      Consumer: ({
        children
      }: {
        children: (props: { branchLike: BranchLike }) => React.ReactNode;
      }) => {
        return children({ branchLike: mockBranch() });
      }
    }
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      event: mockAnalysisEvent({
        category: 'VERSION',
        description: undefined,
        qualityGate: undefined
      })
    })
  ).toMatchSnapshot('no description');
  expect(shallowRender({ event: mockAnalysisEvent() })).toMatchSnapshot('rich quality gate');
  expect(
    shallowRender({
      event: mockAnalysisEvent({
        category: 'DEFINITION_CHANGE',
        definitionChange: {
          projects: [{ changeType: 'ADDED', key: 'foo', name: 'Foo' }]
        },
        qualityGate: undefined
      })
    })
      .find('Consumer')
      .dive()
  ).toMatchSnapshot('definition change');
});

function shallowRender(props: Partial<EventInnerProps> = {}) {
  return shallow(
    <EventInner
      event={mockAnalysisEvent({ category: 'VERSION', qualityGate: undefined })}
      {...props}
    />
  );
}
