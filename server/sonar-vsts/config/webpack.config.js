/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
const webpack = require('webpack');
const paths = require('./paths');
const utils = require('./utils');

module.exports = ({ production = true, fast = false }) => ({
  bail: production,

  devtool: production ? (fast ? false : 'source-map') : 'cheap-module-source-map',
  resolve: {
    // Add '.ts' and '.tsx' as resolvable extensions.
    extensions: ['.ts', '.tsx', '.js', '.json']
  },
  entry: {
    vsts: [
      !production && require.resolve('react-dev-utils/webpackHotDevClient'),
      !production && require.resolve('react-error-overlay'),
      'react',
      'react-dom',
      './src/main/js/index.js'
    ].filter(Boolean)
  },
  output: {
    path: paths.appBuild,
    pathinfo: !production,
    publicPath: '/integration/vsts/',
    filename: production ? 'js/[name].[chunkhash:8].js' : 'js/[name].js',
    chunkFilename: production ? 'js/[name].[chunkhash:8].chunk.js' : 'js/[name].chunk.js'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        loader: 'babel-loader',
        exclude: /(node_modules|libs)/
      },
      {
        test: /\.tsx?$/,
        use: [
          {
            loader: 'awesome-typescript-loader',
            options: {
              transpileOnly: true,
              useBabel: true,
              useCache: true
            }
          }
        ]
      },
      {
        test: /\.css$/,
        use: ['style-loader', utils.cssLoader({ production, fast }), utils.postcssLoader()]
      }
    ].filter(Boolean)
  },
  plugins: [
    !production && new InterpolateHtmlPlugin({ WEB_CONTEXT: '' }),

    new HtmlWebpackPlugin({
      inject: false,
      template: paths.appHtml,
      minify: utils.minifyParams({ production, fast })
    }),

    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(production ? 'production' : 'development')
    }),

    new CopyWebpackPlugin([
      {
        from: './src/main/js/libs/third-party/VSS.SDK.min.js',
        to: 'js/'
      }
    ]),

    production &&
      !fast &&
      new webpack.optimize.UglifyJsPlugin({
        sourceMap: true,
        compress: { screw_ie8: true, warnings: false },
        mangle: { screw_ie8: true },
        output: { comments: false, screw_ie8: true }
      }),

    !production && new webpack.HotModuleReplacementPlugin()
  ].filter(Boolean)
});
