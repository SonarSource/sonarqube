/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/* globals casper: false, exports: false */

var fs = require('fs');

var getPort = function () {
  return casper.cli.options.port;
};

var BASE_URL = 'http://localhost:' + getPort() + '/pages/',
    BASE_ROOT_URL = 'http://localhost:' + getPort(),
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
  console.log('');
  console.log(currentFile);
  console.log('');
  var curFilePath = currentFile.split(fs.separator);
  if (curFilePath.length > 1) {
    curFilePath.pop(); // test name
    curFilePath.pop(); // "js" dir
    curFilePath.push('json');
    curFilePath.push(dir);
    casper.log('changing working dir to: ' + curFilePath.join(fs.separator));
    fs.changeWorkingDirectory(curFilePath.join(fs.separator));
  }
};


exports.configureCasper = function () {
  casper.options.waitTimeout = 30000;
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


function patchWithTimestamp(url) {
  var t = Date.now(),
      hashStart = url.indexOf('#'),
      hash =  hashStart !== -1 ? url.substr(hashStart) : '',
      base = hashStart !== -1 ? url.substr(0, hashStart) : url;
  return base + '?' + t + hash;
}


exports.buildUrl = function (urlTail) {
  return patchWithTimestamp(BASE_URL + urlTail);
};


exports.buildRootUrl = function (urlTail) {
  return patchWithTimestamp(BASE_ROOT_URL + urlTail);
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


exports.sendCoverage = function () {
  return casper.evaluate(function () {
    jQuery.ajax({
      type: 'POST',
      url: '/coverage/client',
      data: JSON.stringify(window.__coverage__),
      processData: false,
      contentType: 'application/json; charset=UTF-8',
      async: false
    });
  });
  casper.wait(500);
};
