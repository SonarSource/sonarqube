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

/*
 This webpack config is actually two: one for modern browsers and one for the legacy ones (e.g. ie11)

 The modern one transpilies the code to ES2018 (i.e. with classes, async functions, etc.) and
 does not include any polyfills. It's included in the result index.html using <script type="module">.
 Legacy browsers ignore this tag.

 The legacy one transpilies the code to ES5 and polyfills ES5+ features (promises, generators, etc.).
 It's included in the result index.html using <script nomodule>. Modern browsers do not load such scripts.
 
 There is a trick to have both scripts in the index.html. We generate this file only once, during the
 build for modern browsers. We want unique file names for each version to invalidate browser cache. 
 For modern browsers we generate a file suffix using the content hash (as previously). For legacy ones
 we can't do the same, because we need to know the file names without the build.

 To work-around the problem, we use a build timestamp which is added to the legacy build file names.
 This way assuming that the build generates exactly the same entry chunks, we know the name of the 
 legacy files. Inside index.html template we use a simple regex to replace the file hash of a modern 
 file name to the timestamp. To simplify the regex we use ".m." suffix for modern files.

 This whole thing might be simplified when (if) the following issue is resolved:
 https://github.com/jantimon/html-webpack-plugin/issues/1051
*/

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
        // This avoid having multi instance of styled component when developing with yarn link on sonar-ui-common
        // See https://www.styled-components.com/docs/faqs#how-can-i-fix-issues-when-using-npm-link-or-yarn-link
        'styled-components': path.resolve(__dirname, '../node_modules/styled-components'),
        // This avoid having multi instance of react when developing with yarn link on sonar-ui-common
        // See https://reactjs.org/warnings/invalid-hook-call-warning.html
        react: path.resolve(__dirname, '../node_modules/react'),
        'react-dom': path.resolve(__dirname, '../node_modules/react-dom')
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
                !assetFilename.endsWith('.map') && !assetFilename.startsWith('js/docs.'),
              maxAssetSize: 300000,
              hints: 'error'
            }
          : undefined
    }),

    Object.assign({ name: 'legacy' }, commonConfig, {
      entry: [
        !production && require.resolve('react-dev-utils/webpackHotDevClient'),
        require.resolve('./polyfills'),
        !production && require.resolve('react-error-overlay'),
        './src/main/js/app/utils/setPublicPath.js',
        './src/main/js/app/index.ts'
      ].filter(Boolean),
      output: {
        path: paths.appBuild,
        pathinfo: !production,
        filename: production ? `js/[name].${timestamp}.js` : 'js/[name].js',
        chunkFilename: production ? `js/[name].${timestamp}.chunk.js` : 'js/[name].chunk.js'
      },
      module: {
        rules: [
          {
            test: /(\.js$|\.ts(x?)$)/,
            exclude: /(node_modules|libs)/,
            use: [
              {
                loader: 'babel-loader',
                options: {
                  configFile: path.join(__dirname, '../babel.config.legacy.js')
                }
              },
              {
                loader: 'ts-loader',
                options: {
                  configFile: 'tsconfig.legacy.json',
                  transpileOnly: true
                }
              }
            ]
          },
          ...commonRules
        ]
      },
      plugins: [...commonPlugins, !production && new webpack.HotModuleReplacementPlugin()].filter(
        Boolean
      )
    })
  ];
};
