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
import ListStyleFacetFooter from '../../../../components/facet/ListStyleFacetFooter';
import { getStandards } from '../../../../helpers/security-standard';
import { click } from '../../../../helpers/testUtils';
import { Query } from '../../utils';
import StandardFacet from '../StandardFacet';

jest.mock('../../../../helpers/security-standard', () => ({
  ...jest.requireActual('../../../../helpers/security-standard'),
  getStandards: jest.fn().mockResolvedValue({
    owaspTop10: {
      a1: {
        title: 'Injection',
      },
      a2: {
        title: 'Broken Authentication',
      },
    },
    'owaspTop10-2021': {
      a1: {
        title: 'Injection',
      },
      a2: {
        title: 'Broken Authentication',
      },
    },
    sansTop25: {
      'insecure-interaction': {
        title: 'Insecure Interaction Between Components',
      },
    },
    cwe: {
      unknown: {
        title: 'No CWE associated',
      },
      '1004': {
        title: "Sensitive Cookie Without 'HttpOnly' Flag",
      },
    },
    sonarsourceSecurity: {
      'sql-injection': {
        title: 'SQL Injection',
      },
      'command-injection': {
        title: 'Command Injection',
      },
    },
  }),
}));

it('should render closed', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(getStandards).not.toHaveBeenCalled();
});

it('should toggle standards facet', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle });
  click(wrapper.children('FacetHeader'));
  expect(onToggle).toHaveBeenCalledWith('standards');
});

it('should clear standards facet', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });
  wrapper.children('FacetHeader').prop<Function>('onClear')();
  expect(onChange).toHaveBeenCalledWith({
    cwe: [],
    owaspTop10: [],
    'owaspTop10-2021': [],
    sansTop25: [],
    sonarsourceSecurity: [],
    standards: [],
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
      sonarsourceSecurityStats: { 'sql-injection': 12 },
    })
  ).toMatchSnapshot();
  expect(getStandards).toHaveBeenCalled();
});

it('should show sonarsource facet more button', () => {
  const wrapper = shallowRender({
    open: true,
    sonarsourceSecurity: ['traceability', 'permission', 'others'],
    sonarsourceSecurityOpen: true,
    sonarsourceSecurityStats: {
      'buffer-overflow': 3,
      'sql-injection': 3,
      rce: 3,
      'object-injection': 3,
      'command-injection': 3,
      'path-traversal-injection': 3,
      'ldap-injection': 3,
      'xpath-injection': 3,
      'expression-lang-injection': 3,
      'log-injection': 3,
      xxe: 3,
      xss: 3,
      dos: 3,
      ssrf: 3,
      csrf: 3,
      'http-response-splitting': 3,
      'open-redirect': 3,
      'weak-cryptography': 3,
      auth: 3,
      'insecure-conf': 3,
      'file-manipulation': 3,
      'encrypt-data': 3,
      traceability: 3,
      permission: 3,
      others: 3,
    },
  });

  expect(wrapper.find(ListStyleFacetFooter).exists()).toBe(true);

  wrapper.setState({ showFullSonarSourceList: true });
  expect(wrapper.find(ListStyleFacetFooter).exists()).toBe(false);
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
    sonarsourceSecurityStats: { 'sql-injection': 10 },
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
    expect(onChange).toHaveBeenLastCalledWith({ [facet]: expectedValue });
  }
});

it('should toggle sub-facets', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle, open: true });
  click(wrapper.find('FacetBox[property="owaspTop10"]').children('FacetHeader'));
  expect(onToggle).toHaveBeenLastCalledWith('owaspTop10');
  click(wrapper.find('FacetBox[property="sansTop25"]').children('FacetHeader'));
  expect(onToggle).toHaveBeenLastCalledWith('sansTop25');
  click(wrapper.find('FacetBox[property="sonarsourceSecurity"]').children('FacetHeader'));
  expect(onToggle).toHaveBeenLastCalledWith('sonarsourceSecurity');
});

it('should display correct selection', () => {
  const wrapper = shallowRender({
    open: true,
    owaspTop10: ['a1', 'a3'],
    'owaspTop10-2021': ['a1', 'a2'],
    sansTop25: ['risky-resource', 'foo'],
    cwe: ['42', '1111', 'unknown'],
    sonarsourceSecurity: ['sql-injection', 'others'],
  });
  checkValues('standards', [
    'SONAR SQL Injection',
    'Others',
    'OWASP A1 - a1 title',
    'OWASP A3',
    'OWASP A1 - a1 title',
    'OWASP A2',
    'SANS Risky Resource Management',
    'SANS foo',
    'CWE-42 - cwe-42 title',
    'CWE-1111',
    'Unknown CWE',
  ]);
  checkValues('owaspTop10', ['A1 - a1 title', 'A3']);
  checkValues('owaspTop10-2021', ['A1 - a1 title', 'A2']);
  checkValues('sansTop25', ['Risky Resource Management', 'foo']);
  checkValues('sonarsourceSecurity', ['SQL Injection', 'Others']);

  function checkValues(property: string, values: string[]) {
    expect(
      wrapper.find(`FacetBox[property="${property}"]`).children('FacetHeader').prop('values')
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
      fetchingOwaspTop10-2021={false}
      fetchingSansTop25={false}
      fetchingSonarSourceSecurity={false}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      owaspTop10={[]}
      owaspTop10Open={false}
      owaspTop10Stats={{}}
      owaspTop10-2021={[]}
      owaspTop10-2021Open={false}
      owaspTop10-2021Stats={{}}
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
      'owaspTop10-2021': { a1: { title: 'a1 title' } },
      sansTop25: { 'risky-resource': { title: 'Risky Resource Management' } },
      cwe: { 42: { title: 'cwe-42 title' }, unknown: { title: 'Unknown CWE' } },
      sonarsourceSecurity: {
        'sql-injection': { title: 'SQL Injection' },
        others: { title: 'Others' },
      },
    },
  });
  return wrapper;
}
