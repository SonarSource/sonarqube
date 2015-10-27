/* eslint no-unused-expressions: 0 */
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import Defaults from '../../src/main/js/apps/permission-templates/permission-template-defaults';
import SetDefaults from '../../src/main/js/apps/permission-templates/permission-template-set-defaults';

let expect = require('chai').expect;
let sinon = require('sinon');

describe('Permission Templates', function () {
  describe('Defaults', () => {
    it('should display one qualifier', () => {
      let permissionTemplate = { defaultFor: ['VW'] },
          topQualifiers = ['TRK', 'VW'];
      let result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-vw')).to.have.length(1);
    });

    it('should display two qualifiers', () => {
      let permissionTemplate = { defaultFor: ['TRK', 'VW'] },
          topQualifiers = ['TRK', 'VW'];
      let result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-vw')).to.have.length(1);
    });

    it('should not display qualifiers', () => {
      let permissionTemplate = { defaultFor: [] },
          topQualifiers = ['TRK', 'VW'];
      let result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-vw')).to.be.empty;
    });

    it('should omit "project" if there is only one qualifier', () => {
      let permissionTemplate = { defaultFor: ['TRK'] },
          topQualifiers = ['TRK'];
      let result = TestUtils.renderIntoDocument(
          <Defaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-qualifier-trk')).to.be.empty;
    });
  });

  describe('SetDefaults', () => {
    var refresh = sinon.spy();

    it('should display a dropdown with one option', () => {
      let permissionTemplate = { defaultFor: ['VW'] },
          topQualifiers = ['TRK', 'VW'];
      let result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(1);
    });

    it('should display a dropdown with two options', () => {
      let permissionTemplate = { defaultFor: [] },
          topQualifiers = ['TRK', 'VW'];
      let result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(2);
    });

    it('should not display a dropdown', () => {
      let permissionTemplate = { defaultFor: ['TRK', 'VW'] },
          topQualifiers = ['TRK', 'VW'];
      let result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.be.empty;
    });

    it('should omit dropdown if there is only one qualifier', () => {
      let permissionTemplate = { defaultFor: [] },
          topQualifiers = ['TRK'];
      let result = TestUtils.renderIntoDocument(
          <SetDefaults permissionTemplate={permissionTemplate} topQualifiers={topQualifiers} refresh={refresh}/>
      );
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'dropdown')).to.be.empty;
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(1);
    });
  });
});
