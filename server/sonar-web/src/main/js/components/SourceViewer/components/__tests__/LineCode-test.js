/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import LineCode from '../LineCode';

it('render code', () => {
  const line = {
    line: 3,
    code: '<span class="k">class</span> <span class="sym sym-1">Foo</span> {'
  };
  const issueLocations = [{ from: 0, to: 5, line: 3 }];
  const wrapper = shallow(
    <LineCode
      highlightedSymbols={['sym1']}
      issues={[{ key: 'issue-1' }, { key: 'issue-2' }]}
      issueLocations={issueLocations}
      line={line}
      onIssueSelect={jest.fn()}
      onSelectLocation={jest.fn()}
      onSymbolClick={jest.fn()}
      onPopupToggle={jest.fn()}
      openPopup={null}
      selectedIssue="issue-1"
      showIssues={true}
    />
  );
  expect(wrapper).toMatchSnapshot();
});
