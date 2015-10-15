var path = require('path');

var gulp = require('gulp');
var concat = require('gulp-concat');
var gulpif = require('gulp-if');
var less = require('gulp-less');
var minifyCss = require('gulp-minify-css');
var sourcemaps = require('gulp-sourcemaps');


module.exports.styles = function (output, production, dev) {
  return gulp.src([
        'src/main/less/jquery-ui.less',
        'src/main/less/select2.less',
        'src/main/less/select2-sonar.less',

        'src/main/less/init.less',
        'src/main/less/components.less',
        'src/main/less/pages.less',

        'src/main/less/style.less',

        'src/main/less/*.less'
      ])
      .pipe(gulpif(dev, sourcemaps.init()))
      .pipe(less())
      .pipe(gulpif(production, minifyCss()))
      .pipe(concat('sonar.css'))
      .pipe(gulpif(dev, sourcemaps.write({ includeContent: true })))
      .pipe(gulp.dest(path.join(output, 'css')));
};