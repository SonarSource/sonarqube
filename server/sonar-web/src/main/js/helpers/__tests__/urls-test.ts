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
import {
  getComponentDrilldownUrl,
  getComponentIssuesUrl,
  getQualityGatesUrl,
  getQualityGateUrl
} from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
const COMPLEX_COMPONENT_KEY_ENCODED = encodeURIComponent(COMPLEX_COMPONENT_KEY);
const METRIC = 'coverage';

describe('#getComponentIssuesUrl', () => {
  it('should work without parameters', () => {
    expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, {})).toEqual({
      pathname: '/project/issues',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });

  it('should work with parameters', () => {
    expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, { resolved: 'false' })).toEqual({
      pathname: '/project/issues',
      query: { id: SIMPLE_COMPONENT_KEY, resolved: 'false' }
    });
  });
});

describe('#getComponentDrilldownUrl', () => {
  it('should return component drilldown url', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: SIMPLE_COMPONENT_KEY, metric: METRIC })
    ).toEqual({
      pathname: '/component_measures',
      query: { id: SIMPLE_COMPONENT_KEY, metric: METRIC }
    });
  });

  it('should not encode component key', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: COMPLEX_COMPONENT_KEY, metric: METRIC })
    ).toEqual({
      pathname: '/component_measures',
      query: { id: COMPLEX_COMPONENT_KEY, metric: METRIC }
    });
  });
});

describe('#getQualityGate(s)Url', () => {
  it('should take organization key into account', () => {
    expect(getQualityGatesUrl()).toEqual({ pathname: '/quality_gates' });
    expect(getQualityGatesUrl('foo')).toEqual({ pathname: '/organizations/foo/quality_gates' });
    expect(getQualityGateUrl('bar')).toEqual({ pathname: '/quality_gates/show/bar' });
    expect(getQualityGateUrl('bar', 'foo')).toEqual({
      pathname: '/organizations/foo/quality_gates/show/bar'
    });
  });

  it('should encode keys', () => {
    expect(getQualityGatesUrl(COMPLEX_COMPONENT_KEY)).toEqual({
      pathname: '/organizations/' + COMPLEX_COMPONENT_KEY_ENCODED + '/quality_gates'
    });
    expect(getQualityGateUrl(COMPLEX_COMPONENT_KEY)).toEqual({
      pathname: '/quality_gates/show/' + COMPLEX_COMPONENT_KEY_ENCODED
    });
  });
});
