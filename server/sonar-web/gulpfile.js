var path = require('path');

var del = require('del');
var gulp = require('gulp');
var gutil = require('gulp-util');
var webpack = require('webpack');

var argv = require('yargs').argv;
var output = argv.output || path.join(__dirname, 'src/main/webapp');

var styles = require('./gulp/styles').styles;
var webpackConfig = require('./webpack.config.js');
webpackConfig.output.path = path.join(output, 'js/bundles');


// Clean

gulp.task('clean', function (done) {
  del([
    path.join(output, 'js'),
    path.join(output, 'css')
  ], done);
});


// Styles

gulp.task('styles:prod', function () {
  return styles(output, true);
});

gulp.task('styles:dev', function () {
  return styles(output, false, true);
});


// Webpack

gulp.task('webpack:prod', function (callback) {
  var webpackProdConfig = Object.create(webpackConfig);
  webpackProdConfig.plugins = webpackProdConfig.plugins.concat(
      new webpack.DefinePlugin({
        'process.env': {
          'NODE_ENV': JSON.stringify('production'),
          'OUTPUT': output
        }
      }),
      new webpack.optimize.DedupePlugin(),
      new webpack.optimize.UglifyJsPlugin()
  );

  webpack(webpackProdConfig, function (err) {
    if (err) {
      throw new gutil.PluginError('webpack:prod', err);
    }
    callback();
  });
});

gulp.task('webpack:dev', function (callback) {
  // run webpack
  webpack(webpackConfig, function (err) {
    if (err) {
      throw new gutil.PluginError('webpack:dev', err);
    }
    callback();
  });
});


// Tasks

gulp.task('build', ['clean', 'styles:prod', 'webpack:prod']);
gulp.task('build:dev', ['clean', 'styles:dev', 'webpack:dev']);
gulp.task('default', ['build']);
