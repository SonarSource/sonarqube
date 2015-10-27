var path = require('path');

var glob = require('glob');
var browserify = require('browserify');
var watchify = require('watchify');
var es = require('event-stream');

var gulp = require('gulp');
var gutil = require('gulp-util');
var buffer = require('vinyl-buffer');
var concat = require('gulp-concat');
var gulpif = require('gulp-if');
var uglify = require('gulp-uglify');

var source = require('vinyl-source-stream');


function getAppName (file) {
  return file
      .substr(0, file.length - 7)
      .substr(17);
}


function doBrowserify (entry, sourceName, dest, customize, output, flags) {
  var bundler = browserify({
    entries: [entry],
    debug: flags.dev,
    cache: {},
    packageCache: {}
  });

  // do all .require(), .external()
  if (typeof customize === 'function') {
    bundler = customize(bundler);
  }

  if (flags.watch) {
    bundler = watchify(bundler);
  }

  var rebundle = function () {
    return bundler.bundle()
        .pipe(source(sourceName))
        .pipe(gulpif(flags.production, buffer()))
        .pipe(gulpif(flags.production, uglify()))
        .pipe(gulp.dest(path.join(output, dest)));
  };

  bundler.on('update', rebundle);

  // logs
  var logBefore = function (ids) {
    var message = '' + ids.length + ' files changed, bundling...';
    gutil.log(message);
    ids.forEach(function (id) {
      gutil.log(gutil.colors.yellow(id));
    });
  };
  var logAfter = function (msg) {
    gutil.log(gutil.colors.green(msg));
  };
  bundler.on('update', logBefore);
  bundler.on('log', logAfter);

  return rebundle();
}


module.exports.main = function (output, production, dev, watch) {
  return doBrowserify(
      'src/main/js/main/app.js',
      'main.js',
      'js/bundles',
      function (bundle) {
        return bundle
            .require('react', { expose: 'react' })
            .require('react-dom', { expose: 'react-dom' })
            .require('backbone', { expose: 'backbone' })
            .require('backbone.marionette', { expose: 'backbone.marionette' })
            .require('moment/min/moment-with-locales', { expose: 'moment' });
      },
      output,
      { production: production, dev: dev, watch: watch });
};


module.exports.apps = function (output, production, dev, watch, done) {
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
                .external('react-dom')
                .external('backbone')
                .external('backbone.marionette')
                .external('moment');
          },
          output,
          { production: production, dev: dev, watch: watch });
    });
    es.merge(tasks).on('end', done);
  });
};


module.exports.widgets = function (output, production, dev, watch) {
  return doBrowserify(
      'src/main/js/widgets/widgets.js',
      'widgets.js',
      'js/bundles',
      function (bundle) {
        return bundle
            .external('react')
            .external('react-dom')
            .external('backbone')
            .external('backbone.marionette')
            .require('moment/min/moment-with-locales', { expose: 'moment' })
            .require('./src/main/js/widgets/issue-filter/widget.js', { expose: 'issue-filter-widget' });
      },
      output,
      { production: production, dev: dev, watch: watch });
};


module.exports.sonar = function (output, production) {
  return gulp.src([
        'src/main/js/libs/translate.js',
        'src/main/js/libs/third-party/jquery.js',
        'src/main/js/libs/third-party/jquery-ui.js',
        'src/main/js/libs/third-party/d3.js',
        'src/main/js/libs/third-party/underscore.js',
        'src/main/js/libs/third-party/select2.js',
        'src/main/js/libs/third-party/keymaster.js',
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
};
