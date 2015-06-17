/* jshint node: true */
var express = require('express');

module.exports = function () {
  var app = express();

  app.get('/api/l10n/index', function (req, res) {
    res.setHeader('Content-Type', 'application/json');
    res.end('{}');
  });

  app.get('/api/generic/long', function (req, res) {
    setTimeout(function () {
      res.setHeader('Content-Type', 'application/json');
      res.end('{}');
    }, 2000);
  });

  app.get('/api/generic/failed', function (req, res) {
    res.setHeader('Content-Type', 'application/json');
    res.status('400');
    res.end('{"errors":[{"msg":"Error Message"}]}');
  });

  return app;
};
