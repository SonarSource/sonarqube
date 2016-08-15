/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
process.env.NODE_ENV = 'development';

var url = require('url');
var express = require('express');
var proxy = require('express-http-proxy');
var webpack = require('webpack');
var chalk = require('chalk');
var config = require('../config/webpack/webpack.config.dev.js');

var app = express();

var DEFAULT_PORT = process.env.PORT || 8080;
var API_HOST = process.env.API_HOST || 'http://localhost:9000';

var compiler;

var friendlySyntaxErrorLabel = 'Syntax error:';

function isLikelyASyntaxError (message) {
  return message.indexOf(friendlySyntaxErrorLabel) !== -1;
}

// This is a little hacky.
// It would be easier if webpack provided a rich error object.

function formatMessage (message) {
  return message
  // Make some common errors shorter:
      .replace(
          // Babel syntax error
          'Module build failed: SyntaxError:',
          friendlySyntaxErrorLabel
      )
      .replace(
          // Webpack file not found error
          /Module not found: Error: Cannot resolve 'file' or 'directory'/,
          'Module not found:'
      )
      // Internal stacks are generally useless so we strip them
      .replace(/^\s*at\s.*:\d+:\d+[\s\)]*\n/gm, '') // at ... ...:x:y
      // Webpack loader names obscure CSS filenames
      .replace('./~/css-loader!./~/postcss-loader!', '');
}

function setupCompiler () {
  compiler = webpack(config);

  compiler.plugin('invalid', function () {
    console.log(chalk.cyan.bold('Compiling...'));
  });

  compiler.plugin('done', function (stats) {
    var hasErrors = stats.hasErrors();
    var hasWarnings = stats.hasWarnings();
    if (!hasErrors && !hasWarnings) {
      console.log(chalk.green.bold('Compiled successfully!'));
      return;
    }

    var json = stats.toJson();
    var formattedErrors = json.errors.map(message =>
        'Error in ' + formatMessage(message)
    );
    var formattedWarnings = json.warnings.map(message =>
        'Warning in ' + formatMessage(message)
    );

    if (hasErrors) {
      console.log(chalk.red.bold('Failed to compile:'));
      console.log();
      if (formattedErrors.some(isLikelyASyntaxError)) {
        // If there are any syntax errors, show just them.
        // This prevents a confusing ESLint parsing error
        // preceding a much more useful Babel syntax error.
        formattedErrors = formattedErrors.filter(isLikelyASyntaxError);
      }
      formattedErrors.forEach(message => {
        console.log(message);
        console.log();
      });
      // If errors exist, ignore warnings.
      return;
    }

    if (hasWarnings) {
      console.log(chalk.yellow('Compiled with warnings.'));
      console.log();
      formattedWarnings.forEach(message => {
        console.log(message);
        console.log();
      });

      console.log('You may use special comments to disable some warnings.');
      console.log('Use ' + chalk.yellow(
              '// eslint-disable-next-line') + ' to ignore the next line.');
      console.log('Use ' + chalk.yellow(
              '/* eslint-disable */') + ' to ignore all warnings in a file.');
    }
  });
}

function runDevServer (port) {
  app.use(require('webpack-dev-middleware')(compiler, {
    noInfo: true,
    quiet: true,
    publicPath: config.output.publicPath
  }));

  app.use(require('webpack-hot-middleware')(compiler, {
    noInfo: true,
    quiet: true
  }));

  app.all('*', proxy(API_HOST, {
    forwardPath: function (req) {
      return url.parse(req.url).path;
    }
  }));

  app.listen(port, 'localhost', function (err) {
    if (err) {
      console.log(err);
      return;
    }

    console.log(chalk.green.bold(
        'The app is running at http://localhost:' + port + '/'));
    console.log(chalk.cyan.bold('Compiling...'));
    console.log();
  });
}

function run (port) {
  setupCompiler();
  runDevServer(port);
}

run(DEFAULT_PORT);

