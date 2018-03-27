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
    extensions: ['.ts', '.tsx', '.js', '.json'],
    alias: {
      '@sqcore': path.resolve(__dirname, '../../sonar-web/src/main/js')
    }
  },
  entry: [
    !production && require.resolve('react-dev-utils/webpackHotDevClient'),
    require.resolve('./polyfills'),
    !production && require.resolve('react-error-overlay'),
    './src/main/ts/index.tsx'
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
        exclude: /node_modules/,
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
        use: ['style-loader', utils.cssLoader({ production }), utils.postcssLoader()]
      }
    ]
  },
  plugins: [
    // `allowExternal: true` to remove files outside of the current dir
    production && new CleanWebpackPlugin([paths.appBuild], { allowExternal: true, verbose: false }),

    new CopyWebpackPlugin([{ from: paths.appPublic, to: paths.appBuild, ignore: [paths.appHtml] }]),

    new HtmlWebpackPlugin({
      inject: false,
      template: paths.appHtml,
      minify: utils.minifyParams({ production })
    }),

    // keep `InterpolateHtmlPlugin` after `HtmlWebpackPlugin`
    !production &&
      new InterpolateHtmlPlugin({
        WEB_CONTEXT: '',
        BBC_APP_KEY: '',
        BBC_JWT: '',
        BBC_WIDGET_KEY: '',
        BBC_PROJECT_KEY: ''
      }),

    !production && new webpack.HotModuleReplacementPlugin()
  ].filter(Boolean)
});
