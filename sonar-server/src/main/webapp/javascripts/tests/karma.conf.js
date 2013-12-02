/* global module:false, karma:false */

// Karma configuration
// Generated on Mon Dec 02 2013 14:50:55 GMT+0600 (YEKT)

module.exports = function(karma) {
  karma.configure({

    // base path, that will be used to resolve files and exclude
    basePath: '..',


    // frameworks to use
    frameworks: ['qunit'],


    // list of files / patterns to load in the browser
    files: [
        // dependencies
        'third-party/jquery.min.js',
        'third-party/underscore-min.js',
        'third-party/backbone-min.js',
        'third-party/backbone.marionette.min.js',

        // app
        'navigator/filters/base-filters.js',

        // tests
        'tests/measures.js'
    ],


    // list of files to exclude
    exclude: [
      
    ],


    preprocessors: {
      'navigator/**/*.js': 'coverage'
    },


    // test results reporter to use
    // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
    reporters: ['progress', 'coverage'],


    coverageReporter: {
      type : 'text',
      dir : 'coverage/'
    },


    // web server port
    port: 9876,


    // cli runner port
    runnerPort: 9100,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: karma.LOG_DISABLE || karma.LOG_ERROR || karma.LOG_WARN || karma.LOG_INFO || karma.LOG_DEBUG
    logLevel: karma.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: ['PhantomJS'],


    // If browser does not capture in given timeout [ms], kill it
    captureTimeout: 60000,


    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: true
  });
};
