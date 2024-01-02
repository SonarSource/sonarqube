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
import { mount } from 'enzyme';
import * as React from 'react';
import { IndexationContext } from '../../../app/components/indexation/IndexationContext';
import { PageContext } from '../../../app/components/indexation/PageUnavailableDueToIndexation';
import { IndexationContextInterface } from '../../../types/indexation';
import withIndexationGuard from '../withIndexationGuard';

it('should not render children because indexation is in progress', () => {
  const wrapper = mountRender();
  expect(wrapper.find(TestComponent).exists()).toBe(false);
});

it('should not render children because indexation has failures', () => {
  const wrapper = mountRender({
    status: { isCompleted: true, percentCompleted: 100, hasFailures: true },
  });
  expect(wrapper.find(TestComponent).exists()).toBe(false);
});

it('should render children because indexation is completed without failures', () => {
  const wrapper = mountRender({
    status: { isCompleted: true, percentCompleted: 100, hasFailures: false },
  });
  expect(wrapper.find(TestComponent).exists()).toBe(true);
});

function mountRender(context?: Partial<IndexationContextInterface>) {
  return mount(
    <IndexationContext.Provider
      value={{
        status: { isCompleted: false, percentCompleted: 23, hasFailures: false },
        ...context,
      }}
    >
      <TestComponentWithGuard />
    </IndexationContext.Provider>
  );
}

class TestComponent extends React.PureComponent {
  render() {
    return <h1>TestComponent</h1>;
  }
}

const TestComponentWithGuard = withIndexationGuard(TestComponent, PageContext.Issues);
