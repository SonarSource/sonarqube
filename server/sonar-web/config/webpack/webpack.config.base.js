const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const paths = require('../paths');

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
      './src/main/js/libs/third-party/bootstrap/tooltip.js',
      './src/main/js/libs/third-party/bootstrap/dropdown.js'
    ],

    app: [
      './src/main/js/app/utils/setPublicPath.js',
      './src/main/js/app/index.js',
      './src/main/js/components/SourceViewer/SourceViewer.js'
    ]
  },
  output: {
    path: paths.appBuild,
    filename: 'js/[name].[chunkhash:8].js',
    chunkFilename: 'js/[name].[chunkhash:8].chunk.js'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        loader: 'babel-loader',
        exclude: /(node_modules|libs)/
      },
      {
        test: /(blueimp-md5|numeral)/,
        loader: 'imports-loader?define=>false'
      },
      {
        test: /\.hbs$/,
        loader: 'handlebars-loader',
        options: {
          helperDirs: path.join(__dirname, '../../src/main/js/helpers/handlebars')
        }
      },
      {
        test: /\.css$/,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'postcss-loader',
            options: {
              plugins() {
                return [require('autoprefixer')];
              }
            }
          }
        ]
      },
      {
        test: /\.less$/,
        use: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: [
            {
              loader: 'css-loader',
              options: { url: false }
            },
            {
              loader: 'postcss-loader',
              options: {
                plugins() {
                  return [require('autoprefixer')];
                }
              }
            },
            'less-loader'
          ]
        })
      },
      { test: require.resolve('jquery'), loader: 'expose-loader?$!expose-loader?jQuery' },
      { test: require.resolve('underscore'), loader: 'expose-loader?_' },
      { test: require.resolve('backbone'), loader: 'expose-loader?Backbone' },
      { test: require.resolve('backbone.marionette'), loader: 'expose-loader?Marionette' },
      { test: require.resolve('react'), loader: 'expose-loader?React' },
      { test: require.resolve('react-dom'), loader: 'expose-loader?ReactDOM' }
    ]
  }
};
