define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  require('intern/order!build/js/libs/translate.js');

  var React = require('react');
  var TestUtils = React.addons.TestUtils;

  var Card = require('build/js/apps/overview/card');
  var Cards = require('build/js/apps/overview/cards');

  bdd.describe('Overview - Card', function () {
    bdd.it('should render .overview-card', function () {
      var result = TestUtils.renderIntoDocument(React.createElement(Card, null));
      assert.ok(TestUtils.findRenderedDOMComponentWithClass(result, 'overview-card'));
    });

    bdd.it('should render children', function () {
      var result = TestUtils.renderIntoDocument(React.createElement(Card, null, '!'));
      assert.equal('!', result.getDOMNode().textContent);
    });
  });

  bdd.describe('Overview - Cards', function () {
    bdd.it('should render .overview-cards', function () {
      var result = TestUtils.renderIntoDocument(React.createElement(Cards, null));
      assert.ok(TestUtils.findRenderedDOMComponentWithClass(result, 'overview-cards'));
    });

    bdd.it('should render children', function () {
      var result = TestUtils.renderIntoDocument(React.createElement(Cards, null, '!'));
      assert.equal('!', result.getDOMNode().textContent);
    });
  });
});
