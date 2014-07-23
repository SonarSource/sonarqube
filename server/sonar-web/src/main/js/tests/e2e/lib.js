var fs = require('fs');


var BASE_URL = 'http://localhost:3000/pages/',
    WINDOW_WIDTH = 1200,
    WINDOW_HEIGHT = 800;


exports.initMessages = function () {
  // Dump log messages
  casper.on('remote.message', function(message) {
    this.echo('Log: '+ message, 'LOG');
  });

  // Dump uncaught errors
  casper.on('page.error', function(msg) {
    this.echo('Error: ' + msg, 'ERROR');
  });
};


exports.changeWorkingDirectory = function (dir) {
  // Since Casper has control, the invoked script is deep in the argument stack
  var currentFile = require('system').args[4];
  var curFilePath = fs.absolute(currentFile).split(fs.separator);
  if (curFilePath.length > 1) {
    curFilePath.pop(); // PhantomJS does not have an equivalent path.baseName()-like method
    curFilePath.push(dir);
    fs.changeWorkingDirectory(curFilePath.join(fs.separator));
  }
};


var mockRequest = function (url, response) {
  casper.evaluate(function (url, response) {
    jQuery.mockjax({ url: url, responseText: response});
  }, url, response);
};
exports.mockRequest = mockRequest;


exports.mockRequestFromFile = function (url, fileName) {
  var response = fs.read(fileName);
  mockRequest(url, response);
};


exports.buildUrl = function (urlTail) {
  return BASE_URL + urlTail;
};


exports.setDefaultViewport = function () {
  casper.viewport(WINDOW_WIDTH, WINDOW_HEIGHT);
};


exports.capture = function (fileName) {
  casper.capture(fileName, { top: 0, left: 0, width: WINDOW_WIDTH, height: WINDOW_HEIGHT });
};

