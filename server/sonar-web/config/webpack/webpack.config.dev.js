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
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
const paths = require('../paths');
const config = require('./webpack.config.base');

const port = 3000;
const host = '0.0.0.0';

config.devtool = 'cheap-module-source-map';

config.entry.vendor.unshift('webpack/hot/only-dev-server');
config.entry.vendor.unshift(`webpack-dev-server/client?http://${host}:${port}`);

config.output.pathinfo = true;
config.output.filename = 'js/[name].js';
config.output.chunkFilename = 'js/[name].chunk.js';

config.module.rules.unshift({
  test: /\.js$/,
  enforce: 'pre',
  loader: 'eslint-loader',
  include: paths.appSrc
});

config.plugins = [
  new webpack.optimize.CommonsChunkPlugin({ name: 'vendor' }),
  new ExtractTextPlugin({ filename: 'css/sonar.css', allChunks: true }),
  new InterpolateHtmlPlugin({ WEB_CONTEXT: '' }),
  new HtmlWebpackPlugin({ inject: false, template: paths.appHtml }),
  new webpack.DefinePlugin({ 'process.env.NODE_ENV': '"development"' }),
  new webpack.HotModuleReplacementPlugin()
];

// docs: https://webpack.js.org/configuration/dev-server/
config.devServer = {
  compress: true,

  contentBase: paths.appPublic,
  historyApiFallback: true,
  proxy: [
    {
      context: ['/api/**', '/fonts/**', '/images/**'],
      target: 'http://localhost:9000'
    }
  ],

  port,
  host,

  hot: true,
  overlay: true,

  quiet: false,
  noInfo: false,
  stats: {
    assets: false,
    colors: true,
    version: false,
    hash: false,
    timings: true,
    chunks: false,
    chunkModules: false
  }
};

module.exports = config;
