/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { ComponentQualifier } from '../../types/component';
import { IssueType } from '../../types/issues';
import {
  getComponentDrilldownUrl,
  getComponentIssuesUrl,
  getComponentOverviewUrl,
  getComponentSecurityHotspotsUrl,
  getIssuesUrl,
  getQualityGatesUrl,
  getQualityGateUrl
} from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
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

describe('getComponentSecurityHotspotsUrl', () => {
  it('should work with no extra parameters', () => {
    expect(getComponentSecurityHotspotsUrl(SIMPLE_COMPONENT_KEY, {})).toEqual({
      pathname: '/security_hotspots',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });

  it('should forward some query parameters', () => {
    expect(
      getComponentSecurityHotspotsUrl(SIMPLE_COMPONENT_KEY, {
        sinceLeakPeriod: 'true',
        ignoredParam: '1234'
      })
    ).toEqual({
      pathname: '/security_hotspots',
      query: { id: SIMPLE_COMPONENT_KEY, sinceLeakPeriod: 'true' }
    });
  });
});

describe('getComponentOverviewUrl', () => {
  it('should return a portfolio url for a portfolio', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Portfolio)).toEqual({
      pathname: '/portfolio',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
  it('should return a portfolio url for a subportfolio', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.SubPortfolio)).toEqual({
      pathname: '/portfolio',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
  it('should return a dashboard url for a project', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Project)).toEqual({
      pathname: '/dashboard',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
  it('should return a dashboard url for an app', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Application)).toEqual({
      pathname: '/dashboard',
      query: { id: SIMPLE_COMPONENT_KEY }
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
  it('should work as expected', () => {
    expect(getQualityGatesUrl()).toEqual({ pathname: '/quality_gates' });
    expect(getQualityGateUrl('bar')).toEqual({ pathname: '/quality_gates/show/bar' });
  });
});

describe('getIssuesUrl', () => {
  it('should work as expected', () => {
    const type = IssueType.Bug;
    expect(getIssuesUrl({ type })).toEqual({
      pathname: '/issues',
      query: { type }
    });
  });
});
