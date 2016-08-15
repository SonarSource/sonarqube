/* eslint no-var: 0 */
var del = require('del');
var gulp = require('gulp');
var gulpif = require('gulp-if');
var less = require('gulp-less');
var nano = require('gulp-cssnano');
var autoprefixer = require('gulp-autoprefixer');
var paths = require('./config/paths');
var autoprefixerOptions = require('./config/autoprefixer');

var nanoOptions = {
  zindex: false,
  discardComments: { removeAll: true }
};

function styles (output, production) {
  return gulp.src(['src/main/less/sonar.less'])
      .pipe(less())
      .pipe(autoprefixer(autoprefixerOptions))
      .pipe(gulpif(production, nano(nanoOptions)))
      .pipe(gulp.dest(output));
}

gulp.task('clean', function (done) {
  del(paths.cssBuild, done);
});

gulp.task('styles:prod', function () {
  return styles(paths.cssBuild, true);
});

gulp.task('styles:dev', function () {
  return styles(paths.cssBuild, false);
});

gulp.task('default', ['clean', 'styles:prod']);
gulp.task('build', ['clean', 'styles:prod']);
gulp.task('build-fast', ['clean', 'styles:dev']);
