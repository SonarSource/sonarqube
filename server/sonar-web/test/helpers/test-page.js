define(function (require) {

  var assert = require('intern/chai!assert');
  var fs = require('intern/dojo/node!fs');
  var Command = require('intern/dojo/node!leadfoot/Command');
  var pollUntil = require('intern/dojo/node!leadfoot/helpers/pollUntil');

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

  Command.prototype.assertElementExist = function (selector) {
    return new this.constructor(this, function () {
      return this.parent
          .findAllByCssSelector(selector)
          .then(function (elements) {
            assert.ok(elements.length, selector + ' exists');
          })
          .end();
    });
  };

  Command.prototype.assertElementNotExist = function (selector) {
    return new this.constructor(this, function () {
      return this.parent
          .findAllByCssSelector(selector)
          .then(function (elements) {
            assert.equal(elements.length, 0, selector + ' does not exist');
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

  Command.prototype.assertElementVisible = function (selector) {
    return new this.constructor(this, function () {
      return this.parent
          .findAllByCssSelector(selector)
          .isDisplayed()
          .then(function (displayed) {
            assert.ok(displayed, selector + ' is visible');
          })
          .end();
    });
  };

  Command.prototype.clickElement = function (selector) {
    return new this.constructor(this, function () {
      return this.parent
          .findByCssSelector(selector)
          .click()
          .end()
          .sleep(250);
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

  Command.prototype.waitForElementCount = function (selector, count) {
    return new this.constructor(this, function () {
      return this.parent
          .then(pollUntil(function (selector, count) {
            var elements = document.querySelectorAll(selector);
            return elements.length === count ? true : null;
          }, [selector, count]));
    });
  };

  Command.prototype.mockFromFile = function (url, file, options) {
    var response = fs.readFileSync('src/test/json/' + file, 'utf-8');
    return new this.constructor(this, function () {
      return this.parent
          .execute(function (url, response, options) {
            return jQuery.mockjax(_.extend({ url: url, responseText: response }, options));
          }, [url, response, options]);
    });
  };

  Command.prototype.mockFromString = function (url, response, options) {
    return new this.constructor(this, function () {
      return this.parent
          .execute(function (url, response, options) {
            return jQuery.mockjax(_.extend({ url: url, responseText: response }, options));
          }, [url, response, options]);
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
          }, [app])
          .sleep(2000);
    });
  };

});
