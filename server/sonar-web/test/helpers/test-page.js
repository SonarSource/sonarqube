define(function (require) {

  var assert = require('intern/chai!assert');
  var fs = require('intern/dojo/node!fs');
  var Command = require('intern/dojo/node!leadfoot/Command');

  Command.prototype.assertElementCount = function (selector, count) {
    return new this.constructor(this, function () {
      return this.parent
          .findAllByCssSelector(selector)
          .then(function (elements) {
            assert.equal(count, elements.length, count + ' elements were found by ' + selector);
          })
          .end();
    });
  };

  Command.prototype.assertElementInclude = function (selector, text) {
    return new this.constructor(this, function () {
      return this.parent
          .findAllByCssSelector(selector)
          .getVisibleText()
          .then(function (texts) {
            assert.include(texts.join(''), text, selector + ' contains "' + text + '"');
          })
          .end();
    });
  };

  Command.prototype.assertElementNotInclude = function (selector, text) {
    return new this.constructor(this, function () {
      return this.parent
          .findAllByCssSelector(selector)
          .getVisibleText()
          .then(function (texts) {
            assert.notInclude(texts.join(''), text, selector + ' does not contain "' + text + '"');
          })
          .end();
    });
  };

  Command.prototype.clickElement = function (selector) {
    return new this.constructor(this, function () {
      return this.parent
          .findByCssSelector(selector)
          .click()
          .end();
    });
  };

  Command.prototype.fillElement = function (selector, value) {
    return new this.constructor(this, function () {
      return this.parent
          .execute(function (selector, value) {
            jQuery(selector).val(value);
          }, [selector, value]);
    });
  };

  Command.prototype.mockFromFile = function (url, file) {
    var response = fs.readFileSync('src/test/json/' + file, 'utf-8');
    return new this.constructor(this, function () {
      return this.parent
          .execute(function (url, response) {
            return jQuery.mockjax(_.extend({ url: url, responseText: response }));
          }, [url, response]);
    });
  };

  Command.prototype.mockFromString = function (url, response) {
    return new this.constructor(this, function () {
      return this.parent
          .execute(function (url, response) {
            return jQuery.mockjax(_.extend({ url: url, responseText: response }));
          }, [url, response]);
    });
  };

  Command.prototype.clearMocks = function () {
    return new this.constructor(this, function () {
      return this.parent
          .execute(function () {
            jQuery.mockjaxClear();
          });
    });
  };

  Command.prototype.startApp = function (app) {
    return new this.constructor(this, function () {
      return this.parent
          .execute(function (app) {
            require(['apps/' + app + '/app'], function (App) {
              App.start({ el: '#content' });
            });
          }, [app]);
    });
  };

});
