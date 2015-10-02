var path = require('path');

var glob = require('glob');
var del = require('del');
var browserify = require('browserify');
var watchify = require('watchify');
var es = require('event-stream');

var gulp = require('gulp');
var buffer = require('vinyl-buffer');
var concat = require('gulp-concat');
var gulpif = require('gulp-if');
var less = require('gulp-less');
var minifyCss = require('gulp-minify-css');
var sourcemaps = require('gulp-sourcemaps');
var uglify = require('gulp-uglify');

var source = require('vinyl-source-stream');

var argv = require('yargs').argv;
var production = !argv.dev && !argv.fast;
var dev = !!argv.dev && !argv.fast;
var watch = !!argv.watch;
var output = argv.output || './src/main/webapp';


function getAppName (file) {
  return file
      .substr(0, file.length - 7)
      .substr(17);
}


function doBrowserify (entry, sourceName, dest, customize) {
  var bundler = browserify({
    entries: [entry],
    debug: dev,
    cache: {},
    packageCache: {}
  });

  // do all .require(), .external()
  if (typeof customize === 'function') {
    bundler = customize(bundler);
  }

  if (watch) {
    bundler = watchify(bundler);
  }

  var rebundle = function (ids) {
    if (ids) {
      /* eslint no-console: 0 */
      console.log(ids);
    }
    return bundler.bundle()
        .pipe(source(sourceName))
        .pipe(gulpif(production, buffer()))
        .pipe(gulpif(production, uglify()))
        .pipe(gulp.dest(path.join(output, dest)));
  };

  bundler.on('update', rebundle);

  return rebundle();
}


gulp.task('scripts-sonar', function () {
  return gulp.src([
    'src/main/js/libs/translate.js',
    'src/main/js/libs/third-party/jquery.js',
    'src/main/js/libs/third-party/jquery-ui.js',
    'src/main/js/libs/third-party/d3.js',
    'src/main/js/libs/third-party/underscore.js',
    'src/main/js/libs/third-party/select2.js',
    'src/main/js/libs/third-party/keymaster.js',
    'src/main/js/libs/third-party/moment.js',
    'src/main/js/libs/third-party/numeral.js',
    'src/main/js/libs/third-party/numeral-languages.js',
    'src/main/js/libs/third-party/bootstrap/tooltip.js',
    'src/main/js/libs/third-party/bootstrap/dropdown.js',
    'src/main/js/libs/select2-jquery-ui-fix.js',

    'src/main/js/libs/graphics/pie-chart.js',
    'src/main/js/libs/graphics/barchart.js',
    'src/main/js/libs/sortable.js',

    'src/main/js/libs/inputs.js',
    'src/main/js/libs/jquery-isolated-scroll.js',

    'src/main/js/libs/application.js'
  ])
      .pipe(concat('sonar.js'))
      .pipe(gulpif(production, buffer()))
      .pipe(gulpif(production, uglify()))
      .pipe(gulp.dest(path.join(output, 'js')));
});


gulp.task('scripts-main', function () {
  return doBrowserify(
      'src/main/js/main/app.js',
      'main.js',
      'js/bundles',
      function (bundle) {
        return bundle
            .require('react', { expose: 'react' })
            .require('backbone', { expose: 'backbone' })
            .require('backbone.marionette', { expose: 'backbone.marionette' });
      });
});


gulp.task('scripts-apps', function (done) {
  glob('src/main/js/apps/*/app.js', function (err, files) {
    if (err) {
      done(err);
    }

    var tasks = files.map(function (entry) {
      return doBrowserify(
          entry,
          getAppName(entry) + '.js',
          'js/bundles',
          function (bundle) {
            return bundle
                .external('react')
                .external('backbone')
                .external('backbone.marionette');
          }
      );
    });
    es.merge(tasks).on('end', done);
  });
});

gulp.task('scripts-widgets', function () {
  return doBrowserify(
      'src/main/js/widgets/widgets.js',
      'widgets.js',
      'js/bundles',
      function (bundle) {
        return bundle
            .external('react')
            .external('backbone')
            .external('backbone.marionette')
            .require('./src/main/js/widgets/issue-filter/widget.js', { expose: 'issue-filter-widget' });
      });
});

gulp.task('styles', function () {
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
});

gulp.task('clean', function (done) {
  del([
    path.join(output, 'js'),
    path.join(output, 'css')
  ], done);
});

gulp.task('scripts', ['scripts-sonar', 'scripts-main', 'scripts-apps', 'scripts-widgets']);

gulp.task('build', ['clean', 'scripts', 'styles'], function () {
  if (watch) {
    gulp.watch('src/main/less/**/*.less', ['styles']);
  }
});

gulp.task('default', ['build']);