const path = require('path');
const autoprefixer = require('autoprefixer');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
const eslintFormatter = require('react-dev-utils/eslintFormatter');
const webpack = require('webpack');
const paths = require('./paths');

module.exports = ({ production = true, fast = false }) => ({
  bail: production,

  devtool: production ? fast ? false : 'source-map' : 'cheap-module-source-map',

  entry: {
    vendor: [
      !production && require.resolve('react-dev-utils/webpackHotDevClient'),
      require.resolve('./polyfills'),
      !production && require.resolve('react-error-overlay'),
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
    ].filter(Boolean),

    app: [
      './src/main/js/app/utils/setPublicPath.js',
      './src/main/js/app/index.js',
      './src/main/js/components/SourceViewer/SourceViewer.js'
    ]
  },
  output: {
    path: paths.appBuild,
    pathinfo: !production,
    filename: production ? 'js/[name].[chunkhash:8].js' : 'js/[name].js',
    chunkFilename: production ? 'js/[name].[chunkhash:8].chunk.js' : 'js/[name].chunk.js'
  },
  module: {
    rules: [
      // First, run the linter.
      // It's important to do this before Babel processes the JS.
      // Run for development or full build
      (!production || !fast) && {
        test: /\.js$/,
        enforce: 'pre',
        include: paths.appSrc,
        use: {
          loader: 'eslint-loader',
          options: { formatter: eslintFormatter }
        }
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        exclude: /(node_modules|libs)/
      },
      {
        test: /\.hbs$/,
        use: [
          {
            loader: 'handlebars-loader',
            options: {
              helperDirs: path.join(__dirname, '../src/main/js/helpers/handlebars')
            }
          }
        ]
      },
      {
        test: /\.css$/,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'postcss-loader',
            options: {
              plugins: () => [autoprefixer]
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
                plugins: () => [autoprefixer]
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
    ].filter(Boolean)
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({ name: 'vendor' }),

    new ExtractTextPlugin({
      filename: production ? 'css/sonar.[chunkhash:8].css' : 'css/sonar.css',
      allChunks: true
    }),

    !production && new InterpolateHtmlPlugin({ WEB_CONTEXT: '' }),

    new HtmlWebpackPlugin({
      inject: false,
      template: paths.appHtml,
      minify: production &&
      !fast && {
        removeComments: true,
        collapseWhitespace: true,
        removeRedundantAttributes: true,
        useShortDoctype: true,
        removeEmptyAttributes: true,
        removeStyleLinkTypeAttributes: true,
        keepClosingSlash: true,
        minifyJS: true,
        minifyCSS: true,
        minifyURLs: true
      }
    }),

    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(production ? 'production' : 'development')
    }),

    production &&
      !fast &&
      new webpack.optimize.UglifyJsPlugin({
        sourceMap: true,
        compress: { screw_ie8: true, warnings: false },
        mangle: { screw_ie8: true },
        output: { comments: false, screw_ie8: true }
      }),

    !production && new webpack.HotModuleReplacementPlugin()
  ].filter(Boolean),
  // Some libraries import Node modules but don't use them in the browser.
  // Tell Webpack to provide empty mocks for them so importing them works.
  node: {
    fs: 'empty',
    net: 'empty',
    tls: 'empty'
  }
});
