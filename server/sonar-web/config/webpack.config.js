const path = require('path');
const autoprefixer = require('autoprefixer');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
const webpack = require('webpack');
const paths = require('./paths');
const autoprefixerOptions = require('./autoprefixer');

module.exports = ({ production = true, fast = false }) => ({
  bail: production,

  devtool: production ? fast ? false : 'source-map' : 'cheap-module-eval-source-map',

  entry: {
    vendor: [
      !production && require.resolve('react-dev-utils/webpackHotDevClient'),
      require.resolve('./polyfills'),
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
    // Run for development or full build
    preLoaders: !production || !fast
      ? [
          {
            test: /\.js$/,
            loader: 'eslint',
            include: paths.appSrc
          }
        ]
      : [],
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
          helperDirs: path.join(__dirname, '../src/main/js/helpers/handlebars')
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
  plugins: [
    new webpack.optimize.CommonsChunkPlugin(
      'vendor',
      production ? 'js/vendor.[chunkhash:8].js' : 'js/vendor.js'
    ),

    new ExtractTextPlugin(production ? 'css/sonar.[chunkhash:8].css' : 'css/sonar.css', {
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

    production && new webpack.optimize.OccurrenceOrderPlugin(),
    production && new webpack.optimize.DedupePlugin(),

    production &&
      !fast &&
      new webpack.optimize.UglifyJsPlugin({
        compress: { screw_ie8: true, warnings: false },
        mangle: { screw_ie8: true },
        output: { comments: false, screw_ie8: true }
      }),

    !production && new webpack.HotModuleReplacementPlugin()
  ].filter(Boolean),
  postcss() {
    return [autoprefixer(autoprefixerOptions)];
  },
  // Some libraries import Node modules but don't use them in the browser.
  // Tell Webpack to provide empty mocks for them so importing them works.
  node: {
    fs: 'empty',
    net: 'empty',
    tls: 'empty'
  }
});
