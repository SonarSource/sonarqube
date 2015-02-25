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
    errorhandler = require('errorhandler'),
    serveStatic = require('serve-static');

var staticPath = path.join(__dirname, '../../../webapp');

var app = express();

app.set('views', __dirname + '/views');
app.set('view engine', 'jade');

app.use(errorhandler({ dumpExceptions: true, showStack: true }));

app.use('/', serveStatic(staticPath));

app.get('/pages/:page', function (req, res) {
  res.render(req.param('page'));
});

// Get the port from environment variables
var port = process.env.PORT || 8000;

app.listen(port);

console.log('Server running on port %d', port);
