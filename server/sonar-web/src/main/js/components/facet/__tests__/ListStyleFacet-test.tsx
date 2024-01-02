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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from '../../../helpers/testUtils';
import ListStyleFacet, { Props } from '../ListStyleFacet';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should select items', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });
  const instance = wrapper.instance() as ListStyleFacet<string>;

  // select one item
  instance.handleItemClick('b', false);
  expect(onChange).toHaveBeenLastCalledWith({ foo: ['b'] });
  wrapper.setProps({ values: ['b'] });

  // select another item
  instance.handleItemClick('a', false);
  expect(onChange).toHaveBeenLastCalledWith({ foo: ['a'] });
  wrapper.setProps({ values: ['a'] });

  // unselect item
  instance.handleItemClick('a', false);
  expect(onChange).toHaveBeenLastCalledWith({ foo: [] });
  wrapper.setProps({ values: [] });

  // select multiple items
  wrapper.setProps({ values: ['b'] });
  instance.handleItemClick('c', true);
  expect(onChange).toHaveBeenLastCalledWith({ foo: ['b', 'c'] });
  wrapper.setProps({ values: ['b', 'c'] });

  // unselect item
  instance.handleItemClick('c', true);
  expect(onChange).toHaveBeenLastCalledWith({ foo: ['b'] });
});

it('should toggle', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle });
  wrapper.find('FacetHeader').prop<Function>('onClick')();
  expect(onToggle).toHaveBeenCalled();
});

it('should clear', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange, values: ['a'] });
  wrapper.find('FacetHeader').prop<Function>('onClear')();
  expect(onChange).toHaveBeenCalledWith({ foo: [] });
});

it('should search', async () => {
  const onSearch = jest.fn().mockResolvedValue({
    results: ['d', 'e'],
    paging: { pageIndex: 1, pageSize: 2, total: 3 },
  });
  const loadSearchResultCount = jest.fn().mockResolvedValue({ d: 7, e: 3 });
  const wrapper = shallowRender({ loadSearchResultCount, onSearch });

  // search
  wrapper.find('SearchBox').prop<Function>('onChange')('query');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onSearch).toHaveBeenLastCalledWith('query');
  expect(loadSearchResultCount).toHaveBeenLastCalledWith(['d', 'e']);

  // load more results
  onSearch.mockResolvedValue({
    results: ['f'],
    paging: { pageIndex: 2, pageSize: 2, total: 3 },
  });
  loadSearchResultCount.mockResolvedValue({ f: 5 });
  wrapper.find('ListFooter').prop<Function>('loadMore')();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onSearch).toHaveBeenLastCalledWith('query', 2);

  // clear search
  onSearch.mockClear();
  loadSearchResultCount.mockClear();
  wrapper.find('SearchBox').prop<Function>('onChange')('');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onSearch).not.toHaveBeenCalled();
  expect(loadSearchResultCount).not.toHaveBeenCalled();

  // search for no results
  onSearch.mockResolvedValue({ results: [], paging: { pageIndex: 1, pageSize: 2, total: 0 } });
  wrapper.find('SearchBox').prop<Function>('onChange')('blabla');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(onSearch).toHaveBeenLastCalledWith('blabla');
  expect(loadSearchResultCount).not.toHaveBeenCalled();

  // search fails
  onSearch.mockRejectedValue(undefined);
  wrapper.find('SearchBox').prop<Function>('onChange')('blabla');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot(); // should render previous results
  expect(onSearch).toHaveBeenLastCalledWith('blabla');
  expect(loadSearchResultCount).not.toHaveBeenCalled();
});

it('should limit the number of items', () => {
  const wrapper = shallowRender({ maxInitialItems: 2, maxItems: 5 });
  expect(wrapper.find('FacetItem').length).toBe(2);

  wrapper.find('ListStyleFacetFooter').prop<Function>('showMore')();
  wrapper.update();
  expect(wrapper.find('FacetItem').length).toBe(3);

  wrapper.find('ListStyleFacetFooter').prop<Function>('showLess')();
  wrapper.update();
  expect(wrapper.find('FacetItem').length).toBe(2);
});

it('should show warning that there might be more results', () => {
  const wrapper = shallowRender({ maxInitialItems: 2, maxItems: 3 });
  wrapper.find('ListStyleFacetFooter').prop<Function>('showMore')();
  wrapper.update();
  expect(wrapper.find('Alert').exists()).toBe(true);
});

it('should reset state when closes', () => {
  const wrapper = shallowRender();
  wrapper.setState({
    query: 'foobar',
    searchResults: ['foo', 'bar'],
    searching: true,
    showFullList: true,
  });

  wrapper.setProps({ open: false });
  checkInitialState(wrapper);
});

it('should reset search when query changes', () => {
  const wrapper = shallowRender({ query: { a: ['foo'] } });
  wrapper.setState({ query: 'foo', searchResults: ['foo'], searchResultsCounts: { foo: 3 } });
  wrapper.setProps({ query: { a: ['foo'], b: ['bar'] } });
  checkInitialState(wrapper);
});

it('should collapse list when new stats have few results', () => {
  const wrapper = shallowRender({ maxInitialItems: 2, maxItems: 3 });
  wrapper.setState({ showFullList: true });

  wrapper.setProps({ stats: { d: 1 } });
  expect(wrapper.state('showFullList')).toBe(false);
});

it('should display all selected items', () => {
  const wrapper = shallowRender({
    maxInitialItems: 2,
    stats: { a: 10, b: 5, c: 3 },
    values: ['a', 'b', 'c'],
  });
  expect(wrapper).toMatchSnapshot();
});

it('should be disabled', () => {
  const wrapper = shallowRender({ disabled: true, disabledHelper: 'Disabled helper description' });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<Props<string>> = {}) {
  return shallow(
    <ListStyleFacet
      facetHeader="facet header"
      fetching={false}
      getFacetItemText={identity}
      getSearchResultKey={identity}
      getSearchResultText={identity}
      onChange={jest.fn()}
      onSearch={jest.fn()}
      onToggle={jest.fn()}
      open={true}
      property="foo"
      renderFacetItem={identity}
      renderSearchResult={identity}
      searchPlaceholder="search for foo..."
      stats={{ a: 10, b: 8, c: 1 }}
      values={[]}
      {...props}
    />
  );
}

function identity(str: string) {
  return str;
}

function checkInitialState(wrapper: ShallowWrapper) {
  expect(wrapper.state('query')).toBe('');
  expect(wrapper.state('searchResults')).toBeUndefined();
  expect(wrapper.state('searching')).toBe(false);
  expect(wrapper.state('searchResultsCounts')).toEqual({});
  expect(wrapper.state('showFullList')).toBe(false);
}
