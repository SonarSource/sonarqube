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
import * as React from 'react';
import { shallow } from 'enzyme';
import SuggestionsProvider from '../SuggestionsProvider';
import { isSonarCloud } from '../../../../helpers/system';

jest.mock(
  'Docs/EmbedDocsSuggestions.json',
  () => ({
    pageA: [{ link: '/foo', text: 'Foo' }, { link: '/bar', text: 'Bar', scope: 'sonarcloud' }],
    pageB: [{ link: '/qux', text: 'Qux' }]
  }),
  { virtual: true }
);

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

it('should add & remove suggestions', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => false);
  const children = jest.fn();
  const wrapper = shallow(<SuggestionsProvider>{children}</SuggestionsProvider>);
  const instance = wrapper.instance() as SuggestionsProvider;
  expect(children).lastCalledWith({ suggestions: [] });

  instance.addSuggestions('pageA');
  expect(children).lastCalledWith({ suggestions: [{ link: '/foo', text: 'Foo' }] });

  instance.addSuggestions('pageB');
  expect(children).lastCalledWith({
    suggestions: [{ link: '/qux', text: 'Qux' }, { link: '/foo', text: 'Foo' }]
  });

  instance.removeSuggestions('pageA');
  expect(children).lastCalledWith({ suggestions: [{ link: '/qux', text: 'Qux' }] });
});

it('should show sonarcloud pages', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const children = jest.fn();
  const wrapper = shallow(<SuggestionsProvider>{children}</SuggestionsProvider>);
  const instance = wrapper.instance() as SuggestionsProvider;
  expect(children).lastCalledWith({ suggestions: [] });

  instance.addSuggestions('pageA');
  expect(children).lastCalledWith({
    suggestions: [{ link: '/foo', text: 'Foo' }, { link: '/bar', text: 'Bar', scope: 'sonarcloud' }]
  });
});
