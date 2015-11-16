var path = require('path');

var del = require('del');

var gulp = require('gulp');
var gutil = require('gulp-util');
var env = require('gulp-env');

var argv = require('yargs').argv;
var production = !argv.dev && !argv.fast;
var dev = !!argv.dev && !argv.fast;
var output = argv.output || './src/main/webapp';
var nodeEnv = production ? 'production' : 'development';

var styles = require('./gulp/styles').styles;
var scripts = require('./gulp/scripts');


gulp.task('set-env', function () {
  env({ vars: { 'NODE_ENV': nodeEnv } });
});

gulp.task('scripts-sonar', function () {
  return scripts.sonar(output, production);
});

gulp.task('scripts-main', function () {
  return scripts.main(output, production, dev, false);
});

gulp.task('scripts-main-watch', function () {
  return scripts.main(output, production, dev, true);
});

gulp.task('scripts-apps', function (done) {
  return scripts.apps(output, production, dev, false, done);
});

gulp.task('scripts-apps-watch', function (done) {
  return scripts.apps(output, production, dev, true, done);
});

gulp.task('scripts-widgets', function () {
  return scripts.widgets(output, production, dev, false);
});

gulp.task('scripts-widgets-watch', function () {
  return scripts.widgets(output, production, dev, true);
});

gulp.task('styles', function () {
  return styles(output, production, dev);
});

gulp.task('clean', function (done) {
  del([
    path.join(output, 'js'),
    path.join(output, 'css')
  ], done);
});

gulp.task('scripts', ['scripts-sonar', 'scripts-main', 'scripts-apps', 'scripts-widgets']);

gulp.task('build', ['clean', 'scripts', 'styles']);

gulp.task('watch', ['scripts-main-watch', 'scripts-apps-watch', 'scripts-widgets-watch'], function () {
  gulp.watch('src/main/less/**/*.less', ['styles']);
  gutil.log(gutil.colors.bgGreen('Watching for changes...'));
});

gulp.task('default', ['set-env', 'build']);
