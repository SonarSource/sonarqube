/* jshint node: true */
var express = require('express'),
    path = require('path'),
    im = require('istanbul-middleware'),
    url = require('url'),
    JS_RE = /\.js$/,
    THIRD_PARTY_RE = /\/third-party\//,
    TEMPLATES_RE = /\/templates.js/;

module.exports = function () {
  var app = express();

  var staticPath = path.join(__dirname, '../../../build');
  im.hookLoader(staticPath);
  app.use(im.createClientHandler(staticPath, {
    matcher: function (req) {
      var parsed = url.parse(req.url);
      return parsed.pathname &&
          parsed.pathname.match(JS_RE) &&
          !parsed.pathname.match(THIRD_PARTY_RE) &&
          !parsed.pathname.match(TEMPLATES_RE);
    }
  }));
  app.use('/coverage', im.createHandler());

  return app;
};
