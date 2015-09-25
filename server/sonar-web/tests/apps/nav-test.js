import React from 'react/addons';
import ComponentNavBreadcrumbs from '../../src/main/js/apps/nav/component/component-nav-breadcrumbs';

let TestUtils = React.addons.TestUtils;
let expect = require('chai').expect;

describe('Nav', function () {
  describe('ComponentNavBreadcrumbs', () => {
    it('should not render unless `props.breadcrumbs`', function () {
      var result = React.renderToStaticMarkup(React.createElement(ComponentNavBreadcrumbs, null));
      expect(result).to.equal('<noscript></noscript>');
    });

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
