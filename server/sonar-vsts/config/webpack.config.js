/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
/* eslint-disable import/no-extraneous-dependencies */
const path = require('path');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');
const InterpolateHtmlPlugin = require('./InterpolateHtmlPlugin');
const paths = require('./paths');
const utils = require('./utils');

module.exports = ({ production = true }) => ({
  mode: production ? 'production' : 'development',
  devtool: production ? 'source-map' : 'cheap-module-source-map',
  resolve: {
    // Add '.ts' and '.tsx' as resolvable extensions.
    extensions: ['.ts', '.tsx', '.js', '.json']
  },
  entry: [
    !production && require.resolve('react-dev-utils/webpackHotDevClient'),
    require.resolve('./polyfills'),
    !production && require.resolve('react-error-overlay'),
    './src/main/js/index.js'
  ].filter(Boolean),
  output: {
    path: paths.appBuild,
    pathinfo: !production,
    publicPath: paths.publicPath,
    filename: production ? 'js/[name].[chunkhash:8].js' : 'js/[name].js',
    chunkFilename: production ? 'js/[name].[chunkhash:8].chunk.js' : 'js/[name].chunk.js'
  },
  module: {
    rules: [
      {
        test: /(\.js$|\.ts(x?)$)/,
        exclude: /(node_modules|libs)/,
        use: [
          { loader: 'babel-loader' },
          {
            loader: 'ts-loader',
            options: { transpileOnly: true }
          }
        ]
      },
      {
        test: /\.css$/,
        use: ['style-loader', utils.cssLoader({ production }), utils.postcssLoader()].filter(
          Boolean
        )
      }
    ].filter(Boolean)
  },
  plugins: [
    production && new CleanWebpackPlugin(),

    production &&
      new CopyWebpackPlugin([
        { from: paths.appPublic, to: paths.appBuild, ignore: [paths.appHtml] },
        { from: paths.jsLib, to: paths.jsBuild }
      ]),

    new HtmlWebpackPlugin({
      inject: false,
      template: paths.appHtml,
      minify: utils.minifyParams({ production })
    }),

    // keep `InterpolateHtmlPlugin` after `HtmlWebpackPlugin`
    !production && new InterpolateHtmlPlugin({ WEB_CONTEXT: '' }),

    !production && new webpack.HotModuleReplacementPlugin()
  ].filter(Boolean)
});
