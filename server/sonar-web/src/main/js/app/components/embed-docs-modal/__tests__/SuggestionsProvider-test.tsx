/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { isSonarCloud } from '../../../../helpers/system';
import SuggestionsProvider from '../SuggestionsProvider';

jest.mock(
  'Docs/EmbedDocsSuggestions.json',
  () => ({
    default: {
      pageA: [{ link: '/foo', text: 'Foo' }, { link: '/bar', text: 'Bar', scope: 'sonarcloud' }],
      pageB: [{ link: '/qux', text: 'Qux' }]
    }
  }),
  { virtual: true }
);

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

it('should add & remove suggestions', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(false);
  const wrapper = shallow<SuggestionsProvider>(
    <SuggestionsProvider>
      <div />
    </SuggestionsProvider>
  );
  const instance = wrapper.instance();
  expect(wrapper.state('suggestions')).toEqual([]);

  instance.addSuggestions('pageA');
  expect(wrapper.state('suggestions')).toEqual([{ link: '/foo', text: 'Foo' }]);

  instance.addSuggestions('pageB');
  expect(wrapper.state('suggestions')).toEqual([
    { link: '/qux', text: 'Qux' },
    { link: '/foo', text: 'Foo' }
  ]);

  instance.removeSuggestions('pageA');
  expect(wrapper.state('suggestions')).toEqual([{ link: '/qux', text: 'Qux' }]);
});

it('should show sonarcloud pages', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  const wrapper = shallow<SuggestionsProvider>(
    <SuggestionsProvider>
      <div />
    </SuggestionsProvider>
  );
  const instance = wrapper.instance();
  expect(wrapper.state('suggestions')).toEqual([]);

  instance.addSuggestions('pageA');
  expect(wrapper.state('suggestions')).toEqual([
    { link: '/foo', text: 'Foo' },
    { link: '/bar', text: 'Bar', scope: 'sonarcloud' }
  ]);
});
