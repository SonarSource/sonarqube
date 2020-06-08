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
import { ComponentQualifier } from '../../../../types/component';
import { PageContext, PageUnavailableDueToIndexation } from '../PageUnavailableDueToIndexation';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should refresh the page once the indexation is complete', () => {
  const reload = jest.fn();
  delete window.location;
  (window as any).location = { reload };

  const wrapper = shallowRender();

  expect(reload).not.toHaveBeenCalled();

  wrapper.setProps({ indexationContext: { status: { isCompleted: true } } });
  wrapper.update();

  expect(reload).toHaveBeenCalled();
});

function shallowRender(props?: PageUnavailableDueToIndexation['props']) {
  return shallow(
    <PageUnavailableDueToIndexation
      indexationContext={{
        status: { isCompleted: false }
      }}
      pageContext={PageContext.Issues}
      component={{ qualifier: ComponentQualifier.Portfolio, name: 'test-portfolio' }}
      {...props}
    />
  );
}
