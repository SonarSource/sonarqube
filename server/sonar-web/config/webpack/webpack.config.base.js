/* eslint no-var: 0 */
var path = require('path');
var autoprefixer = require('autoprefixer');
var webpack = require('webpack');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
var url = require('url');
var paths = require('../paths');
var autoprefixerOptions = require('../autoprefixer');

module.exports = {
  entry: {
    'vendor': [
      require.resolve('../polyfills'),
      'jquery',
      'underscore',
      'd3',
      'react',
      'react-dom',
      'backbone',
      'backbone.marionette',
      'moment',
      'handlebars/runtime'
    ],

    'sonar': './src/main/js/libs/sonar.js',

    'main': './src/main/js/main/app.js',
    'app': './src/main/js/app/index.js',

    // should not use js
    'markdown': './src/main/js/apps/markdown/app.js',

    // not unique url
    'source-viewer': './src/main/js/apps/source-viewer/app.js'
  },
  output: {
    path: paths.appBuild,
    filename: '[name].js'
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.js'),
    new ExtractTextPlugin('../../css/sonar.css', { allChunks: true })
  ],
  resolve: {
    // This allows you to set a fallback for where Webpack should look for modules.
    // We read `NODE_PATH` environment variable in `paths.js` and pass paths here.
    // We use `fallback` instead of `root` because we want `node_modules` to "win"
    // if there any conflicts. This matches Node resolution mechanism.
    // https://github.com/facebookincubator/create-react-app/issues/253
    fallback: paths.nodePaths
  },
  module: {
    // First, run the linter.
    // It's important to do this before Babel processes the JS.
    preLoaders: [
      {
        test: /\.js$/,
        loader: 'eslint',
        include: paths.appSrc
      }
    ],
    loaders: [
      {
        test: /\.js$/,
        loader: 'babel',
        exclude: /(node_modules|libs)/,
      },
      {
        test: /(blueimp-md5|numeral)/,
        loader: 'imports?define=>false'
      },
      {
        test: /\.hbs$/,
        loader: 'handlebars',
        query: {
          helperDirs: path.join(__dirname, '../../src/main/js/helpers/handlebars')
        }
      },
      {
        test: /\.css$/,
        loader: 'style!css!postcss'
      },
      {
        test: /\.less$/,
        loader: ExtractTextPlugin.extract('style', 'css?-url!postcss!less')
      },
      { test: require.resolve('jquery'), loader: 'expose?$!expose?jQuery' },
      { test: require.resolve('underscore'), loader: 'expose?_' },
      { test: require.resolve('backbone'), loader: 'expose?Backbone' },
      { test: require.resolve('backbone.marionette'), loader: 'expose?Marionette' },
      { test: require.resolve('d3'), loader: 'expose?d3' },
      { test: require.resolve('react'), loader: 'expose?React' },
      { test: require.resolve('react-dom'), loader: 'expose?ReactDOM' }
    ]
  },
  postcss: function () {
    return [autoprefixer(autoprefixerOptions)];
  },
  // Some libraries import Node modules but don't use them in the browser.
  // Tell Webpack to provide empty mocks for them so importing them works.
  node: {
    fs: 'empty',
    net: 'empty',
    tls: 'empty'
  }
};
