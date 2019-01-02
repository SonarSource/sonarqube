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
import * as React from 'react';
import { shallow } from 'enzyme';
import StandardFacet, { Props } from '../StandardFacet';
import { click } from '../../../../helpers/testUtils';
import { Query } from '../../utils';

it('should render closed', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should toggle standards facet', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle });
  click(wrapper.children('FacetHeader'));
  expect(onToggle).toBeCalledWith('standards');
});

it('should clear standards facet', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });
  wrapper.children('FacetHeader').prop<Function>('onClear')();
  expect(onChange).toBeCalledWith({ cwe: [], owaspTop10: [], sansTop25: [], standards: [] });
});

it('should render sub-facets', () => {
  expect(
    shallowRender({
      cwe: ['42'],
      cweOpen: true,
      cweStats: { 42: 5, 173: 3 },
      open: true,
      owaspTop10: ['a3'],
      owaspTop10Open: true,
      owaspTop10Stats: { a1: 15, a3: 5 },
      sansTop25: ['risky-resource'],
      sansTop25Open: true,
      sansTop25Stats: { foo: 12, 'risky-resource': 10 }
    })
  ).toMatchSnapshot();
});

it('should render empty sub-facet', () => {
  expect(
    shallowRender({ open: true, sansTop25: [], sansTop25Open: true, sansTop25Stats: {} }).find(
      'FacetBox[property="sansTop25"]'
    )
  ).toMatchSnapshot();
});

it('should select items', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({
    cwe: ['42'],
    cweOpen: true,
    cweStats: { 42: 5, 173: 3 },
    onChange,
    open: true,
    owaspTop10: ['a3'],
    owaspTop10Open: true,
    owaspTop10Stats: { a1: 15, a3: 5 },
    sansTop25: ['risky-resource'],
    sansTop25Open: true,
    sansTop25Stats: { foo: 12, 'risky-resource': 10 }
  });

  selectAndCheck('owaspTop10', 'a1');
  selectAndCheck('owaspTop10', 'a1', true, ['a1', 'a3']);
  selectAndCheck('sansTop25', 'foo');

  function selectAndCheck(facet: string, value: string, multiple = false, expectedValue = [value]) {
    wrapper
      .find(`FacetBox[property="${facet}"]`)
      .find(`FacetItem[value="${value}"]`)
      .prop<Function>('onClick')(value, multiple);
    expect(onChange).lastCalledWith({ [facet]: expectedValue });
  }
});

it('should toggle sub-facets', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle, open: true });
  click(wrapper.find('FacetBox[property="owaspTop10"]').children('FacetHeader'));
  expect(onToggle).lastCalledWith('owaspTop10');
  click(wrapper.find('FacetBox[property="sansTop25"]').children('FacetHeader'));
  expect(onToggle).lastCalledWith('sansTop25');
});

it('should display correct selection', () => {
  const wrapper = shallowRender({
    open: true,
    owaspTop10: ['a1', 'a3', 'unknown'],
    sansTop25: ['risky-resource', 'foo'],
    cwe: ['42', '1111', 'unknown']
  });
  checkValues('standards', [
    'OWASP A1 - a1 title',
    'OWASP A3',
    'Not OWAPS',
    'SANS Risky Resource Management',
    'SANS foo',
    'CWE-42 - cwe-42 title',
    'CWE-1111',
    'Unknown CWE'
  ]);
  checkValues('owaspTop10', ['A1 - a1 title', 'A3', 'Not OWAPS']);
  checkValues('sansTop25', ['Risky Resource Management', 'foo']);

  function checkValues(property: string, values: string[]) {
    expect(
      wrapper
        .find(`FacetBox[property="${property}"]`)
        .children('FacetHeader')
        .prop('values')
    ).toEqual(values);
  }
});

function shallowRender(props: Partial<Props> = {}) {
  const wrapper = shallow(
    <StandardFacet
      cwe={[]}
      cweOpen={false}
      cweStats={{}}
      fetchingCwe={false}
      fetchingOwaspTop10={false}
      fetchingSansTop25={false}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      owaspTop10={[]}
      owaspTop10Open={false}
      owaspTop10Stats={{}}
      query={{} as Query}
      sansTop25={[]}
      sansTop25Open={false}
      sansTop25Stats={{}}
      {...props}
    />,
    // disable loading of standards.json
    { disableLifecycleMethods: true }
  );
  wrapper.setState({
    standards: {
      owaspTop10: { a1: { title: 'a1 title' }, unknown: { title: 'Not OWAPS' } },
      sansTop25: { 'risky-resource': { title: 'Risky Resource Management' } },
      cwe: { 42: { title: 'cwe-42 title' }, unknown: { title: 'Unknown CWE' } }
    }
  });
  return wrapper;
}
