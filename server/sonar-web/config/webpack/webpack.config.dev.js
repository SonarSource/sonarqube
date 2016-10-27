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
var webpack = require('webpack');
var CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');
var WatchMissingNodeModulesPlugin = require('react-dev-utils/WatchMissingNodeModulesPlugin');
var paths = require('../paths');
var config = require('./webpack.config.base');
var getClientEnvironment = require('../env');

// Webpack uses `publicPath` to determine where the app is being served from.
var publicPath = '/js/bundles/';

// Get environment variables to inject into our app.
var env = getClientEnvironment();

// This makes the bundle appear split into separate modules in the devtools.
// We don't use source maps here because they can be confusing:
// https://github.com/facebookincubator/create-react-app/issues/343#issuecomment-237241875
// You may want 'cheap-module-source-map' instead if you prefer source maps.
config.devtool = 'eval';

// Include an alternative client for WebpackDevServer. A client's job is to
// connect to WebpackDevServer by a socket and get notified about changes.
// When you save a file, the client will either apply hot updates (in case
// of CSS changes), or refresh the page (in case of JS changes). When you
// make a syntax error, this client will display a syntax error overlay.
// Note: instead of the default WebpackDevServer client, we use a custom one
// to bring better experience for Create React App users. You can replace
// the line below with these two lines if you prefer the stock client:
// require.resolve('webpack-dev-server/client') + '?/',
// require.resolve('webpack/hot/dev-server'),
config.entry.vendor.unshift(require.resolve('react-dev-utils/webpackHotDevClient'));

// Add /* filename */ comments to generated require()s in the output.
config.output.pathinfo = true;

// This is the URL that app is served from.
config.output.publicPath = publicPath;

config.plugins = [].concat(config.plugins, [
  // Makes some environment variables available to the JS code, for example:
  // if (process.env.NODE_ENV === 'development') { ... }. See `./env.js`.
  new webpack.DefinePlugin(env),
  // This is necessary to emit hot updates (currently CSS only):
  new webpack.HotModuleReplacementPlugin(),
  // Watcher doesn't work well if you mistype casing in a path so we use
  // a plugin that prints an error when you attempt to do this.
  // See https://github.com/facebookincubator/create-react-app/issues/240
  new CaseSensitivePathsPlugin(),
  // If you require a missing module and then `npm install` it, you still have
  // to restart the development server for Webpack to discover it. This plugin
  // makes the discovery automatic so you don't have to restart.
  // See https://github.com/facebookincubator/create-react-app/issues/186
  new WatchMissingNodeModulesPlugin(paths.appNodeModules)
]);

module.exports = config;
