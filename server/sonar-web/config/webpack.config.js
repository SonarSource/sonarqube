/* eslint-disable import/no-extraneous-dependencies */
const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
const webpack = require('webpack');
const paths = require('./paths');

const cssMinimizeOptions = {
  discardComments: { removeAll: true }
};

const cssLoader = ({ production, fast }) => ({
  loader: 'css-loader',
  options: {
    importLoaders: 1,
    minimize: production && !fast && cssMinimizeOptions,
    url: false
  }
});

const postcssLoader = () => ({
  loader: 'postcss-loader',
  options: {
    ident: 'postcss',
    plugins: () => [
      require('autoprefixer'),
      require('postcss-custom-properties')({
        variables: require('../src/main/js/app/theme')
      }),
      require('postcss-calc')
    ]
  }
});

module.exports = ({ production = true, fast = false }) => ({
  bail: production,

  devtool: production ? (fast ? false : 'source-map') : 'cheap-module-source-map',
  resolve: {
    // Add '.ts' and '.tsx' as resolvable extensions.
    extensions: ['.ts', '.tsx', '.js', '.json']
  },
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
      production
        ? {
            test: /\.css$/,
            loader: ExtractTextPlugin.extract({
              fallback: 'style-loader',
              use: [cssLoader({ production, fast }), postcssLoader()]
            })
          }
        : {
            test: /\.css$/,
            use: ['style-loader', cssLoader({ production, fast }), postcssLoader()]
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

    production &&
      new ExtractTextPlugin({
        filename: production ? 'css/sonar.[chunkhash:8].css' : 'css/sonar.css'
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
