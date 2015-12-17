var path = require('path');
var webpack = require('webpack');

var baseOutput = process.env.OUTPUT || path.join(__dirname, 'src/main/webapp');
var output = path.join(baseOutput, 'js/bundles');

module.exports = {
  entry: {
    'vendor': [
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

    'account': './src/main/js/apps/account/app.js',
    'api-documentation': './src/main/js/apps/api-documentation/app.js',
    'background-tasks': './src/main/js/apps/background-tasks/app.js',
    'code': './src/main/js/apps/code/app.js',
    'coding-rules': './src/main/js/apps/coding-rules/app.js',
    'component-issues': './src/main/js/apps/component-issues/app.js',
    'custom-measures': './src/main/js/apps/custom-measures/app.js',
    'dashboard': './src/main/js/apps/dashboard/app.js',
    'drilldown': './src/main/js/apps/drilldown/app.js',
    'global-permissions': './src/main/js/apps/global-permissions/app.js',
    'groups': './src/main/js/apps/groups/app.js',
    'issues': './src/main/js/apps/issues/app.js',
    'maintenance': './src/main/js/apps/maintenance/app.js',
    'markdown': './src/main/js/apps/markdown/app.js',
    'measures': './src/main/js/apps/measures/app.js',
    'metrics': './src/main/js/apps/metrics/app.js',
    'overview': './src/main/js/apps/overview/app.js',
    'permission-templates': './src/main/js/apps/permission-templates/app.js',
    'project-permissions': './src/main/js/apps/project-permissions/app.js',
    'projects': './src/main/js/apps/projects/app.js',
    'quality-gates': './src/main/js/apps/quality-gates/app.js',
    'quality-profiles': './src/main/js/apps/quality-profiles/app.js',
    'source-viewer': './src/main/js/apps/source-viewer/app.js',
    'system': './src/main/js/apps/system/app.js',
    'update-center': './src/main/js/apps/update-center/app.js',
    'users': './src/main/js/apps/users/app.js',

    'widgets': './src/main/js/widgets/widgets.js'
  },
  output: {
    path: output,
    filename: '[name].js'
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.js')
  ],
  resolve: {
    root: path.join(__dirname, 'src/main/js')
  },
  module: {
    loaders: [
      {
        test: /\.js$/,
        loader: 'babel',
        exclude: /(node_modules|libs)/
      },
      {
        test: /(blueimp-md5)/,
        loader: 'imports?define=>false'
      },
      {
        test: /\.hbs$/,
        loader: 'handlebars-loader',
        query: {
          helperDirs: path.join(__dirname, 'src/main/js/helpers/handlebars')
        }
      },
      {
        test: require.resolve('jquery'),
        loader: 'expose?$!expose?jQuery'
      },
      {
        test: require.resolve('underscore'),
        loader: 'expose?_'
      },
      {
        test: require.resolve('d3'),
        loader: 'expose?d3'
      }
    ]
  },
  externals: {
    'numeral': 'numeral'
  }
};
