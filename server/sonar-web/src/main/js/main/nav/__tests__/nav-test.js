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
import React from 'react';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import ComponentNavBreadcrumbs from '../component/component-nav-breadcrumbs';

describe('Nav', function () {
  describe('ComponentNavBreadcrumbs', () => {
    it('should not render breadcrumbs with one element', function () {
      const breadcrumbs = [
        { key: 'my-project', name: 'My Project', qualifier: 'TRK' }
      ];
      const result = TestUtils.renderIntoDocument(
          React.createElement(ComponentNavBreadcrumbs, { breadcrumbs })
      );
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'li')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(1);
    });
  });
});
