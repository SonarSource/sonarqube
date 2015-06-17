/* jshint node: true */
var express = require('express'),
    bodyParser = require('body-parser');

module.exports = function () {
  var app = express(),
      mocks = [];

  app.use(bodyParser.json({ limit: '100mb' }));

  app.post('/mock', function (req, res) {
    var url = req.body.url;

    mocks = mocks.filter(function (mock) {
      return mock.url !== url;
    });
    mocks.push(req.body);

    res.status('204');
    res.end('{}');
  });

  app.post('/unmock', function (req, res) {
    var url = req.body.url;

    mocks = mocks.filter(function (mock) {
      return mock.url !== url;
    });

    res.status('204');
    res.end('{}');
  });

  app.get('/api/*', function (req, res) {
    var mock;
    mocks.forEach(function (m) {
      mock = m.url === req.url && m;
    });
    if (mock) {
      res.status('200');
      res.setHeader('Content-Type', 'application/json');
      res.end(mock.response);
    } else {
      res.status('404');
      res.end();
    }
  });

  return app;
};
