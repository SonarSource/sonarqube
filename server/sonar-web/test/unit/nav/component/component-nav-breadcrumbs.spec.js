define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  var React = require('react');
  var TestUtils = React.addons.TestUtils;

  var ComponentNavBreadcrumbs = require('build/js/apps/nav/component/component-nav-breadcrumbs');

  bdd.describe('ComponentNavBreadcrumbs', function () {
    bdd.it('should not render unless `props.breadcrumbs`', function () {
      var result = React.renderToStaticMarkup(React.createElement(ComponentNavBreadcrumbs, null));
      assert.equal(result, '<noscript></noscript>');
    });

    bdd.it('should not render breadcrumbs with one element', function () {
      var breadcrumbs = [
        { key: 'my-project', name: 'My Project', qualifier: 'TRK' }
      ];
      var result = TestUtils.renderIntoDocument(
          React.createElement(ComponentNavBreadcrumbs, { breadcrumbs: breadcrumbs })
      );
      assert.equal(TestUtils.scryRenderedDOMComponentsWithTag(result, 'li').length, 1);
      assert.equal(TestUtils.scryRenderedDOMComponentsWithTag(result, 'a').length, 1);
    });
  });
});
