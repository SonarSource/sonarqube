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
var express = require('express'),
    path = require('path'),

    im = require('istanbul-middleware'),
    url = require('url'),
    JS_RE = /\.js$/,
    THIRD_PARTY_RE = /\/third-party\//,
    TEMPLATES_RE = /\/templates\//;

var app = express();


// Views
app.set('views', __dirname + '/views');
app.set('view engine', 'jade');


// Coverage
var staticPath = path.join(__dirname, '../main/webapp');
im.hookLoader(staticPath);
app.use(im.createClientHandler(staticPath, {
  matcher: function (req) {
    var parsed = url.parse(req.url);
    return parsed.pathname && parsed.pathname.match(JS_RE) &&
        !parsed.pathname.match(THIRD_PARTY_RE) && 
        !parsed.pathname.match(TEMPLATES_RE);
  }
}));
app.use('/coverage', im.createHandler());


// Static
app.use('/js', express.static(path.join(staticPath, 'js')));
app.use('/css', express.static(path.join(staticPath, 'css')));


// API
app.get('/api/l10n/index', function (req, res) {
  res.setHeader('Content-Type', 'application/json');
  res.end('{}');
});
app.get('/api/generic/long', function (req, res) {
  setTimeout(function() {
    res.setHeader('Content-Type', 'application/json');
    res.end('{}');
  }, 2000);
});
app.get('/api/generic/failed', function (req, res) {
  res.setHeader('Content-Type', 'application/json');
  res.status('400');
  res.end('{"errors":[{"msg":"Error Message"}]}');
});


// Pages
app.get('/pages/:page', function (req, res) {
  res.render(req.param('page'));
});


// Get the port from environment variables
var port = process.env.PORT || 8000;

app.listen(port);

console.log('Server running on port %d', port);
