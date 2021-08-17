/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
/* eslint-disable import/no-extraneous-dependencies, complexity */
const path = require('path');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const LodashPlugin = require('lodash-webpack-plugin');
const webpack = require('webpack');
const InterpolateHtmlPlugin = require('./InterpolateHtmlPlugin');
const paths = require('./paths');
const utils = require('./utils');

module.exports = ({ production = true, release = false }) => {
  const timestamp = Date.now();

  const commonConfig = {
    mode: production ? 'production' : 'development',
    devtool: production && release ? 'source-map' : 'cheap-module-source-map',
    resolve: {
      // Add '.ts' and '.tsx' as resolvable extensions.
      extensions: ['.ts', '.tsx', '.js', '.json'],
      // import from 'Docs/foo.md' is rewritten to import from 'sonar-docs/src/foo.md'
      alias: {
        Docs: path.resolve(__dirname, '../../sonar-docs/src'),
        // d3-selection exports an event object, which requires live-binding.
        // In order to support this, we need to tell Webpack to NOT look into
        // the dist/ folder of this module, but in the src/ folder instead.
        // See https://github.com/d3/d3-selection#event
        'd3-selection': path.resolve(__dirname, '../node_modules/d3-selection/src/index.js')
      }
    },
    optimization: {
      splitChunks: {
        chunks: 'all',
        automaticNameDelimiter: '-'
      },
      minimize: production && release
    }
  };

  const commonRules = [
    {
      // extract styles from 'app/' into separate file
      test: /\.css$/,
      include: path.resolve(__dirname, '../src/main/js/app/styles'),
      use: [
        production ? MiniCssExtractPlugin.loader : 'style-loader',
        utils.cssLoader(),
        utils.postcssLoader(production)
      ]
    },
    {
      // inline all other styles
      test: /\.css$/,
      exclude: path.resolve(__dirname, '../src/main/js/app/styles'),
      use: ['style-loader', utils.cssLoader(), utils.postcssLoader(production)]
    },
    {
      test: /\.md$/,
      use: 'raw-loader'
    },
    { test: require.resolve('react'), loader: 'expose-loader?React' },
    { test: require.resolve('react-dom'), loader: 'expose-loader?ReactDOM' },
    {
      test: /\.directory-loader\.js$/,
      loader: path.resolve(__dirname, 'documentation-loader/index.js')
    }
  ];

  const commonPlugins = [
    production &&
      new MiniCssExtractPlugin({
        filename: 'css/[name].[chunkhash:8].css',
        chunkFilename: 'css/[name].[chunkhash:8].chunk.css'
      }),

    new LodashPlugin({
      // keep these features
      // https://github.com/lodash/lodash-webpack-plugin#feature-sets
      shorthands: true,
      collections: true,
      exotics: true, // used to compare "exotic" values, like dates
      memoizing: true,
      flattening: true
    })
  ];

  return [
    Object.assign({ name: 'modern' }, commonConfig, {
      entry: [
        !production && require.resolve('react-dev-utils/webpackHotDevClient'),
        !production && require.resolve('react-error-overlay'),
        './src/main/js/app/utils/setPublicPath.js',
        './src/main/js/app/index.ts'
      ].filter(Boolean),
      output: {
        path: paths.appBuild,
        pathinfo: !production,
        filename: production ? 'js/[name].m.[chunkhash:8].js' : 'js/[name].js',
        chunkFilename: production ? 'js/[name].m.[chunkhash:8].chunk.js' : 'js/[name].chunk.js'
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
          ...commonRules
        ]
      },
      plugins: [
        production && new CleanWebpackPlugin(),

        production &&
          new CopyWebpackPlugin([
            {
              from: paths.docImages,
              to: paths.appBuild + '/images/embed-doc/images'
            }
          ]),

        production &&
          new CopyWebpackPlugin([
            {
              from: paths.appPublic,
              to: paths.appBuild,
              ignore: [paths.appHtml]
            }
          ]),

        ...commonPlugins,

        new HtmlWebpackPlugin({
          inject: false,
          template: paths.appHtml,
          minify: utils.minifyParams({ production: production && release }),
          timestamp
        }),

        // keep `InterpolateHtmlPlugin` after `HtmlWebpackPlugin`
        !production &&
          new InterpolateHtmlPlugin({
            WEB_CONTEXT: process.env.WEB_CONTEXT || '',
            SERVER_STATUS: process.env.SERVER_STATUS || 'UP',
            INSTANCE: process.env.INSTANCE || 'SonarQube',
            OFFICIAL: process.env.OFFICIAL || 'true'
          }),

        !production && new webpack.HotModuleReplacementPlugin()
      ].filter(Boolean),
      performance:
        production && release
          ? {
              // ignore source maps and documentation chunk
              assetFilter: assetFilename =>
                !assetFilename.endsWith('.map') && !assetFilename.startsWith('js/docs'),
              maxAssetSize: 400000,
              hints: 'error'
            }
          : undefined
    })
  ];
};
