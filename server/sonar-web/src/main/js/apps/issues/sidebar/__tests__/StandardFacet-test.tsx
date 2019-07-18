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
import { click } from 'sonar-ui-common/helpers/testUtils';
import { getStandards } from '../../../../helpers/security-standard';
import { Query } from '../../utils';
import StandardFacet from '../StandardFacet';

jest.mock('../../../../helpers/security-standard', () => ({
  ...require.requireActual('../../../../helpers/security-standard'),
  getStandards: jest.fn().mockResolvedValue({
    owaspTop10: {
      a1: {
        title: 'Injection'
      },
      a2: {
        title: 'Broken Authentication'
      }
    },
    sansTop25: {
      'insecure-interaction': {
        title: 'Insecure Interaction Between Components'
      }
    },
    cwe: {
      unknown: {
        title: 'No CWE associated'
      },
      '1004': {
        title: "Sensitive Cookie Without 'HttpOnly' Flag"
      }
    },
    sonarsourceSecurity: {
      'sql-injection': {
        title: 'SQL Injection'
      },
      'command-injection': {
        title: 'Command Injection'
      }
    }
  })
}));

it('should render closed', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(getStandards).not.toBeCalled();
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
  expect(onChange).toBeCalledWith({
    cwe: [],
    owaspTop10: [],
    sansTop25: [],
    sonarsourceSecurity: [],
    standards: []
  });
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
      sansTop25Stats: { foo: 12, 'risky-resource': 10 },
      sonarsourceSecurity: ['sql-injection'],
      sonarsourceSecurityOpen: true,
      sonarsourceSecurityStats: { 'sql-injection': 12 }
    })
  ).toMatchSnapshot();
  expect(getStandards).toBeCalled();
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
    sansTop25Stats: { foo: 12, 'risky-resource': 10 },
    sonarsourceSecurity: ['command-injection'],
    sonarsourceSecurityOpen: true,
    sonarsourceSecurityStats: { 'sql-injection': 10 }
  });

  selectAndCheck('owaspTop10', 'a1');
  selectAndCheck('owaspTop10', 'a1', true, ['a1', 'a3']);
  selectAndCheck('sansTop25', 'foo');
  selectAndCheck('sonarsourceSecurity', 'sql-injection');

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
  click(wrapper.find('FacetBox[property="sonarsourceSecurity"]').children('FacetHeader'));
  expect(onToggle).lastCalledWith('sonarsourceSecurity');
});

it('should display correct selection', () => {
  const wrapper = shallowRender({
    open: true,
    owaspTop10: ['a1', 'a3'],
    sansTop25: ['risky-resource', 'foo'],
    cwe: ['42', '1111', 'unknown'],
    sonarsourceSecurity: ['sql-injection', 'others']
  });
  checkValues('standards', [
    'SONAR SQL Injection',
    'Others',
    'OWASP A1 - a1 title',
    'OWASP A3',
    'SANS Risky Resource Management',
    'SANS foo',
    'CWE-42 - cwe-42 title',
    'CWE-1111',
    'Unknown CWE'
  ]);
  checkValues('owaspTop10', ['A1 - a1 title', 'A3']);
  checkValues('sansTop25', ['Risky Resource Management', 'foo']);
  checkValues('sonarsourceSecurity', ['SQL Injection', 'Others']);

  function checkValues(property: string, values: string[]) {
    expect(
      wrapper
        .find(`FacetBox[property="${property}"]`)
        .children('FacetHeader')
        .prop('values')
    ).toEqual(values);
  }
});

function shallowRender(props: Partial<StandardFacet['props']> = {}) {
  const wrapper = shallow(
    <StandardFacet
      cwe={[]}
      cweOpen={false}
      cweStats={{}}
      fetchingCwe={false}
      fetchingOwaspTop10={false}
      fetchingSansTop25={false}
      fetchingSonarSourceSecurity={false}
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
      sonarsourceSecurity={[]}
      sonarsourceSecurityOpen={false}
      sonarsourceSecurityStats={{}}
      {...props}
    />
  );
  wrapper.setState({
    standards: {
      owaspTop10: { a1: { title: 'a1 title' } },
      sansTop25: { 'risky-resource': { title: 'Risky Resource Management' } },
      cwe: { 42: { title: 'cwe-42 title' }, unknown: { title: 'Unknown CWE' } },
      sonarsourceSecurity: {
        'sql-injection': { title: 'SQL Injection' },
        others: { title: 'Others' }
      }
    }
  });
  return wrapper;
}
