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
/* eslint no-unused-expressions: 0 */
import React from 'react';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';
import sinon from 'sinon';

import Defaults from '../permission-template-defaults';
import SetDefaults from '../permission-template-set-defaults';

describe('Permission Templates', function () {
  describe('Defaults', () => {
    it('should display one qualifier', () => {
      const permissionTemplate = { defaultFor: ['VW'] };
      const topQualifiers = ['TRK', 'VW'];
      const result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-vw')).to.have.length(1);
    });

    it('should display two qualifiers', () => {
      const permissionTemplate = { defaultFor: ['TRK', 'VW'] };
      const topQualifiers = ['TRK', 'VW'];
      const result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-vw')).to.have.length(1);
    });

    it('should not display qualifiers', () => {
      const permissionTemplate = { defaultFor: [] };
      const topQualifiers = ['TRK', 'VW'];
      const result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-vw')).to.be.empty;
    });

    it('should omit "project" if there is only one qualifier', () => {
      const permissionTemplate = { defaultFor: ['TRK'] };
      const topQualifiers = ['TRK'];
      const result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.be.empty;
    });
  });

  describe('SetDefaults', () => {
    const refresh = sinon.spy();

    it('should display a dropdown with one option', () => {
      const permissionTemplate = { defaultFor: ['VW'] };
      const topQualifiers = ['TRK', 'VW'];
      const result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(1);
    });

    it('should display a dropdown with two options', () => {
      const permissionTemplate = { defaultFor: [] };
      const topQualifiers = ['TRK', 'VW'];
      const result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(2);
    });

    it('should not display a dropdown', () => {
      const permissionTemplate = { defaultFor: ['TRK', 'VW'] };
      const topQualifiers = ['TRK', 'VW'];
      const result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.be.empty;
    });

    it('should omit dropdown if there is only one qualifier', () => {
      const permissionTemplate = { defaultFor: [] };
      const topQualifiers = ['TRK'];
      const result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(1);
    });
  });
});
