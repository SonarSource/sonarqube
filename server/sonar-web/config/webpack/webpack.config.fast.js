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
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var config = require('./webpack.config.base');
var getClientEnvironment = require('../env');
var paths = require('../paths');

// Get environment variables to inject into our app.
var env = getClientEnvironment();

// disable eslint loader
config.module.preLoaders = [];

// Don't attempt to continue if there are any errors.
config.bail = true;

config.plugins = [
  new webpack.optimize.CommonsChunkPlugin('vendor', 'js/vendor.[chunkhash:8].js'),

  new ExtractTextPlugin('css/sonar.[chunkhash:8].css', { allChunks: true }),

  new HtmlWebpackPlugin({
    inject: false,
    template: paths.appHtml
  })
];

module.exports = config;
