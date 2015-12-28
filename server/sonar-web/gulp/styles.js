var path = require('path');

var gulp = require('gulp');
var gulpif = require('gulp-if');
var less = require('gulp-less');
var nano = require('gulp-cssnano');
var autoprefixer = require('gulp-autoprefixer');


var nanoOptions = {
  discardComments: { removeAll: true }
};

var autoprefixerOptions = {
  browsers: [
    'last 3 Chrome versions',
    'last 3 Firefox versions',
    'Safari >= 8',
    'Edge >= 12',
    'IE 11'
  ]
};


module.exports.styles = function (output, production) {
  return gulp.src(['src/main/less/sonar.less'])
      .pipe(less())
      .pipe(autoprefixer(autoprefixerOptions))
      .pipe(gulpif(production, nano(nanoOptions)))
      .pipe(gulp.dest(path.join(output, 'css')));
};
