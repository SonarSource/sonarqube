import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ComponentNavBreadcrumbs from '../src/main/js/main/nav/component/component-nav-breadcrumbs';


let expect = require('chai').expect;


describe('Nav', function () {
  describe('ComponentNavBreadcrumbs', () => {
    it('should not render breadcrumbs with one element', function () {
      var breadcrumbs = [
        { key: 'my-project', name: 'My Project', qualifier: 'TRK' }
      ];
      var result = TestUtils.renderIntoDocument(
          React.createElement(ComponentNavBreadcrumbs, { breadcrumbs: breadcrumbs })
      );
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'li')).to.have.length(1);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a')).to.have.length(1);
    });
  });
});
