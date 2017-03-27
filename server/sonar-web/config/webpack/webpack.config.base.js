var path = require('path');
var autoprefixer = require('autoprefixer');
var webpack = require('webpack');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var paths = require('../paths');
var autoprefixerOptions = require('../autoprefixer');

module.exports = {
  entry: {
    vendor: [
      require.resolve('../polyfills'),
      'jquery',
      'underscore',
      'lodash',
      'd3-array',
      'd3-hierarchy',
      'd3-scale',
      'd3-selection',
      'd3-shape',
      'react',
      'react-dom',
      'backbone',
      'backbone.marionette',
      'moment',
      'handlebars/runtime',
      './src/main/js/libs/third-party/jquery-ui.js',
      './src/main/js/libs/third-party/select2.js',
      './src/main/js/libs/third-party/keymaster.js',
      './src/main/js/libs/third-party/bootstrap/tooltip.js',
      './src/main/js/libs/third-party/bootstrap/dropdown.js'
    ],

    app: ['./src/main/js/app/index.js', './src/main/js/components/SourceViewer/SourceViewer.js']
  },
  output: {
    path: paths.appBuild,
    publicPath: '/',
    filename: 'js/[name].[chunkhash:8].js',
    chunkFilename: 'js/[name].[chunkhash:8].chunk.js'
  },
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
        exclude: /(node_modules|libs)/
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
      { test: require.resolve('react'), loader: 'expose?React' },
      { test: require.resolve('react-dom'), loader: 'expose?ReactDOM' }
    ]
  },
  postcss: function() {
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
