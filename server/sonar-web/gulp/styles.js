var path = require('path');

var gulp = require('gulp');
var concat = require('gulp-concat');
var gulpif = require('gulp-if');
var less = require('gulp-less');
var nano = require('gulp-cssnano');
var autoprefixer = require('gulp-autoprefixer');


module.exports.styles = function (output, production) {
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
      .pipe(less())
      .pipe(autoprefixer({
        browsers: [
          'last 3 Chrome versions',
          'last 3 Firefox versions',
          'Safari >= 8',
          'Edge >= 12',
          'IE 11'
        ]
      }))
      .pipe(gulpif(production, nano()))
      .pipe(concat('sonar.css'))
      .pipe(gulp.dest(path.join(output, 'css')));
};
