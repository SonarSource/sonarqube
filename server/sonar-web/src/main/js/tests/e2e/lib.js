/* globals casper: false, exports: false */

var fs = require('fs');

var getPort = function () {
  return casper.cli.options.port;
};

var BASE_URL = 'http://localhost:' + getPort() + '/pages/',
    WINDOW_WIDTH = 1200,
    WINDOW_HEIGHT = 800;


exports.initMessages = function () {
  if (casper.cli.options.verbose) {
    // Dump log messages
    casper.removeAllListeners('remote.message');
    casper.on('remote.message', function (message) {
      this.echo('Log: ' + message, 'LOG');
    });

    // Dump uncaught errors
    casper.removeAllListeners('page.error');
    casper.on('page.error', function (msg) {
      this.echo('Error: ' + msg, 'ERROR');
    });
  }
};


exports.changeWorkingDirectory = function (dir) {
  var commandLineArgs = require('system').args;
  // Since Casper has control, the invoked script is deep in the argument stack
  // commandLineArgs = casper/bin/bootstrap.js,--casper-path=.../casperjs,--cli,--test,[file(s) under test],[options]
  var currentFile = commandLineArgs[4];
  var curFilePath = fs.absolute(currentFile).split(fs.separator);
  if (curFilePath.length > 1) {
    curFilePath.pop(); // PhantomJS does not have an equivalent path.baseName()-like method
    curFilePath.push(dir);
    fs.changeWorkingDirectory(curFilePath.join(fs.separator));
  }
};


exports.testName = function () {
  var head = Array.prototype.slice.call(arguments, 0);
  return function () {
    var tail = Array.prototype.slice.call(arguments, 0),
        body = head.concat(tail);
    return body.join(' :: ');
  };
};


var mockRequest = function (url, response, options) {
  return casper.evaluate(function (url, response, options) {
    return jQuery.mockjax(_.extend({ url: url, responseText: response}, options));
  }, url, response, options || {});
};
exports.mockRequest = mockRequest;


exports.mockRequestFromFile = function (url, fileName, options) {
  var response = fs.read(fileName);
  return mockRequest(url, response, options);
};


exports.clearRequestMocks = function () {
  casper.evaluate(function() {
    jQuery.mockjaxClear();
  });
};


exports.clearRequestMock = function (mockId) {
  casper.evaluate(function(mockId) {
    jQuery.mockjaxClear(mockId);
  }, mockId);
};


exports.buildUrl = function (urlTail) {
  return BASE_URL + urlTail;
};


exports.setDefaultViewport = function () {
  casper.viewport(WINDOW_WIDTH, WINDOW_HEIGHT);
};


exports.capture = function (fileName) {
  if (!fileName) {
    fileName = 'screenshot.png';
  }
  casper.capture(fileName, { top: 0, left: 0, width: WINDOW_WIDTH, height: WINDOW_HEIGHT });
};


exports.waitForElementCount = function (selector, count, callback) {
  return casper.waitFor(function () {
    return casper.evaluate(function (selector, count) {
      return document.querySelectorAll(selector).length === count;
    }, selector, count);
  }, callback);
};

exports.assertLinkHref = function assertElementCount(selector, href, message) {
  var linkHref = this.casper.evaluate(function(selector) {
      return document.querySelector(selector);
  }, selector);
  return this.assert(elementCount === count, message, {
    type: "assertElementCount",
    standard: f('%d element%s matching selector "%s" found',
        count,
        count > 1 ? 's' : '',
        selector),
    values: {
      selector: selector,
      expected: count,
      obtained: elementCount
    }
  });
};
