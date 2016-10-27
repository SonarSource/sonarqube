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

    'background-tasks': './src/main/js/apps/background-tasks/app.js',
    'code': './src/main/js/apps/code/app.js',
    'coding-rules': './src/main/js/apps/coding-rules/app.js',
    'component-issues': './src/main/js/apps/component-issues/app.js',
    'component-measures': './src/main/js/apps/component-measures/app.js',
    'custom-measures': './src/main/js/apps/custom-measures/app.js',
    'dashboard': './src/main/js/apps/dashboard/app.js',
    'global-permissions': './src/main/js/apps/permissions/global/app.js',
    'groups': './src/main/js/apps/groups/app.js',
    'issues': './src/main/js/apps/issues/app.js',
    'maintenance': './src/main/js/apps/maintenance/app.js',
    'markdown': './src/main/js/apps/markdown/app.js',
    'measures': './src/main/js/apps/measures/app.js',
    'metrics': './src/main/js/apps/metrics/app.js',
    'overview': './src/main/js/apps/overview/app.js',
    'permission-templates': './src/main/js/apps/permission-templates/app.js',
    'project-admin': './src/main/js/apps/project-admin/app.js',
    'project-permissions': './src/main/js/apps/permissions/project/app.js',
    'projects-admin': './src/main/js/apps/projects-admin/app.js',
    'quality-gates': './src/main/js/apps/quality-gates/app.js',
    'quality-profiles': './src/main/js/apps/quality-profiles/app.js',
    'settings': './src/main/js/apps/settings/app.js',
    'source-viewer': './src/main/js/apps/source-viewer/app.js',
    'system': './src/main/js/apps/system/app.js',
    'update-center': './src/main/js/apps/update-center/app.js',
    'users': './src/main/js/apps/users/app.js',
    'web-api': './src/main/js/apps/web-api/app.js',

    'widgets': './src/main/js/widgets/widgets.js'
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
