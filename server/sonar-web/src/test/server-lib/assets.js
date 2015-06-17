/* jshint node: true */
var express = require('express'),
    path = require('path');

module.exports = function () {
  var app = express(),
      staticPath = path.join(__dirname, '../../../build');

  app.use('/js', express.static(path.join(staticPath, 'js')));
  app.use('/css', express.static(path.join(staticPath, 'css')));

  return app;
};
