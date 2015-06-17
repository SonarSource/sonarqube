/* jshint node: true */
var express = require('express'),
    path = require('path');

module.exports = function () {
  var app = express();

  app.set('views', path.join(__dirname, '../views'));
  app.set('view engine', 'jade');

  app.get('/pages/:page', function (req, res) {
    res.render(req.param('page'));
  });

  return app;
};
