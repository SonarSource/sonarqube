/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { expect } from 'chai';

import { getComponentUrl, getComponentIssuesUrl, getComponentDrilldownUrl } from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
const COMPLEX_COMPONENT_KEY_ENCODED = encodeURIComponent(COMPLEX_COMPONENT_KEY);
const METRIC = 'coverage';

describe('URLs', function () {
  let oldBaseUrl;

  beforeEach(function () {
    oldBaseUrl = window.baseUrl;
  });

  afterEach(function () {
    window.baseUrl = oldBaseUrl;
  });

  describe('#getComponentUrl', function () {
    it('should return component url', function () {
      expect(getComponentUrl(SIMPLE_COMPONENT_KEY)).to.equal('/dashboard?id=' + SIMPLE_COMPONENT_KEY);
    });

    it('should encode component key', function () {
      expect(getComponentUrl(COMPLEX_COMPONENT_KEY)).to.equal('/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED);
    });

    it('should take baseUrl into account', function () {
      window.baseUrl = '/context';
      expect(getComponentUrl(COMPLEX_COMPONENT_KEY)).to.equal('/context/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED);
    });
  });

  describe('#getComponentIssuesUrl', function () {
    it('should work without parameters', function () {
      expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, {})).to.equal(
          '/component_issues?id=' + SIMPLE_COMPONENT_KEY + '#');
    });

    it('should encode component key', function () {
      expect(getComponentIssuesUrl(COMPLEX_COMPONENT_KEY, {})).to.equal(
          '/component_issues?id=' + COMPLEX_COMPONENT_KEY_ENCODED + '#');
    });

    it('should work with parameters', function () {
      expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, { resolved: 'false' })).to.equal(
          '/component_issues?id=' + SIMPLE_COMPONENT_KEY + '#resolved=false');
    });

    it('should encode parameters', function () {
      expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, { componentUuids: COMPLEX_COMPONENT_KEY })).to.equal(
          '/component_issues?id=' + SIMPLE_COMPONENT_KEY + '#componentUuids=' + COMPLEX_COMPONENT_KEY_ENCODED);
    });

    it('should take baseUrl into account', function () {
      window.baseUrl = '/context';
      expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, {})).to.equal(
          '/context/component_issues?id=' + SIMPLE_COMPONENT_KEY + '#');
    });
  });

  describe('#getComponentDrilldownUrl', function () {
    it('should return component drilldown url', function () {
      expect(getComponentDrilldownUrl(SIMPLE_COMPONENT_KEY, METRIC)).to.equal(
          '/component_measures/metric/' + METRIC + '?id=' + SIMPLE_COMPONENT_KEY);
    });

    it('should encode component key', function () {
      expect(getComponentDrilldownUrl(COMPLEX_COMPONENT_KEY, METRIC)).to.equal(
          '/component_measures/metric/' + METRIC + '?id=' + COMPLEX_COMPONENT_KEY_ENCODED);
    });

    it('should take baseUrl into account', function () {
      window.baseUrl = '/context';
      expect(getComponentDrilldownUrl(SIMPLE_COMPONENT_KEY, METRIC)).to.equal(
          '/context/component_measures/metric/' + METRIC + '?id=' + SIMPLE_COMPONENT_KEY);
    });
  });
});
