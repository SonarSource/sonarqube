module.exports = (grunt) ->
  require('jit-grunt')(grunt, {
    express: 'grunt-express-server'
    unzip: 'grunt-zip'
    replace: 'grunt-text-replace'
  });

  pkg = grunt.file.readJSON('package.json')
  expressPort = '<%= grunt.option("port") || 3000 %>'
  isWindows = process.platform == 'win32'

  grunt.initConfig
    pkg: grunt.file.readJSON('package.json')

    less:
      dev:
        files:
          '<%= grunt.option("assetsDir") || pkg.assets %>css/sonar.css': [
            '<%= pkg.sources %>less/jquery-ui.less'
            '<%= pkg.sources %>less/select2.less'
            '<%= pkg.sources %>less/select2-sonar.less'

            '<%= pkg.sources %>less/init.less'
            '<%= pkg.sources %>less/components.less'
            '<%= pkg.sources %>less/pages.less'

            '<%= pkg.sources %>less/style.less'

            '<%= pkg.sources %>less/*.less'
          ]
      build:
        options:
          cleancss: true
        files:
          '<%= grunt.option("assetsDir") || pkg.assets %>css/sonar.css': [
            '<%= pkg.sources %>less/jquery-ui.less'
            '<%= pkg.sources %>less/select2.less'
            '<%= pkg.sources %>less/select2-sonar.less'

            '<%= pkg.sources %>less/init.less'
            '<%= pkg.sources %>less/components.less'
            '<%= pkg.sources %>less/pages.less'

            '<%= pkg.sources %>less/style.less'

            '<%= pkg.sources %>less/*.less'
          ]


    cssUrlRewrite:
      build:
        src: '<%= grunt.option("assetsDir") || pkg.assets %>css/sonar.css'
        dest: '<%= grunt.option("assetsDir") || pkg.assets %>css/sonar.css'
        options:
          skipExternal: true
          rewriteUrl: (url, options, dataURI) ->
            path = url.replace pkg.assets, ''
            if path.indexOf('data:') == 0
              "#{path}"
            else
              hash = require('crypto').createHash('md5').update(dataURI).digest('hex')
              "../#{path}?#{hash}"



    coffee:
      build:
        files: [
          expand: true
          cwd: '<%= pkg.sources %>coffee'
          src: ['**/*.coffee']
          dest: '<%= grunt.option("assetsDir") || pkg.assets %>js'
          ext: '.js'
        ]


    concat:
      dev:
        files:
          '<%= grunt.option("assetsDir") || pkg.assets %>js/sonar.js': [
            '<%= grunt.option("assetsDir") || pkg.assets %>js/translate.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/jquery.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/jquery-ui.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/d3.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/latinize.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/underscore.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/backbone.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/backbone.marionette.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/handlebars.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/underscore.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/select2.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/keymaster.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/moment.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/numeral.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/numeral-languages.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/bootstrap/tooltip.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/bootstrap/dropdown.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/base.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/widget.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/bubble-chart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/timeline.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/stack-area.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/pie-chart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/histogram.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/word-cloud.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/tag-cloud.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/treemap.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/graphics/pie-chart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/graphics/timeline.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/graphics/barchart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/sortable.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/inputs.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/dialogs.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/processes.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/jquery-isolated-scroll.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/handlebars-extensions.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/application.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/csv.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/dashboard.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/recent-history.js'
          ]
      build:
        files:
          '<%= grunt.option("assetsDir") || pkg.assets %>build/js/sonar.js': [
            '<%= grunt.option("assetsDir") || pkg.assets %>js/translate.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/jquery.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/jquery-ui.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/d3.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/latinize.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/underscore.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/backbone.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/backbone.marionette.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/handlebars.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/underscore.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/select2.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/keymaster.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/moment.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/numeral.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/numeral-languages.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/bootstrap/tooltip.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/third-party/bootstrap/dropdown.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/base.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/widget.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/bubble-chart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/timeline.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/stack-area.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/pie-chart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/histogram.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/word-cloud.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/tag-cloud.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/widgets/treemap.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/graphics/pie-chart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/graphics/timeline.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/graphics/barchart.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/sortable.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/inputs.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/dialogs.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/processes.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/jquery-isolated-scroll.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/common/handlebars-extensions.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/application.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/csv.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/dashboard.js'
            '<%= grunt.option("assetsDir") || pkg.assets %>js/recent-history.js'
          ]


    requirejs:
      options:
        baseUrl: '<%= grunt.option("assetsDir") || pkg.assets %>js/'
        preserveLicenseComments: false
        optimize: 'none'

        paths:
          'backbone': 'third-party/backbone'
          'backbone.marionette': 'third-party/backbone.marionette'
          'handlebars': 'third-party/handlebars'
          'moment': 'third-party/moment'
          'select-list': 'common/select-list'
          'jquery.mockjax': 'third-party/jquery.mockjax'

        shim:
          'backbone.marionette':
            deps: ['backbone']
            exports: 'Marionette'
          'backbone': exports: 'Backbone'
          'handlebars': exports: 'Handlebars'
          'moment': exports: 'moment'
          'select-list': exports: 'SelectList'

      qualityGate: options:
        name: 'quality-gate/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/quality-gate/app.js'

      codingRules: options:
        name: 'coding-rules/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/coding-rules/app.js'

      issues: options:
        name: 'issues/app-new'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/issues/app-new.js'

      issuesContext: options:
        name: 'issues/app-context'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/issues/app-context.js'

      measures: options:
        name: 'measures/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/measures/app.js'

      selectList: options:
        name: 'common/select-list'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/common/select-list.js'

      apiDocumentation: options:
        name: 'api-documentation/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/api-documentation/app.js'

      drilldown: options:
        name: 'drilldown/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/drilldown/app.js'

      dashboard: options:
        name: 'dashboard/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/dashboard/app.js'

      sourceViewer: options:
        name: 'source-viewer/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/source-viewer/app.js'

      design: options:
        name: 'design/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/design/app.js'

      libraries: options:
        name: 'libraries/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/libraries/app.js'

      monitoring: options:
        name: 'analysis-reports/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/analysis-reports/app.js'

      nav: options:
        name: 'nav/app'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/nav/app.js'

      issueFilterWidget: options:
        name: 'widgets/issue-filter'
        out: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/widgets/issue-filter.js'


    handlebars:
      options:
        namespace: 'Templates'
        processName: (name) ->
          pieces = name.split '/'
          fileName = pieces[pieces.length - 1]
          fileName.split('.')[0]
        processPartialName: (name) ->
          pieces = name.split '/'
          fileName = pieces[pieces.length - 1]
          fileName.split('.')[0]

      build:
        files:
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/navigator.js': [
            '<%= pkg.sources %>hbs/navigator/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/coding-rules.js': [
            '<%= pkg.sources %>hbs/common/**/*.hbs'
            '<%= pkg.sources %>hbs/coding-rules/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/quality-gates.js': [
            '<%= pkg.sources %>hbs/quality-gates/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/source-viewer.js': [
            '<%= pkg.sources %>hbs/source-viewer/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/issue.js': [
            '<%= pkg.sources %>hbs/common/**/*.hbs'
            '<%= pkg.sources %>hbs/issue/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/issues.js': [
            '<%= pkg.sources %>hbs/issues/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/api-documentation.js': [
            '<%= pkg.sources %>hbs/api-documentation/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/design.js': [
            '<%= pkg.sources %>hbs/design/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/libraries.js': [
            '<%= pkg.sources %>hbs/libraries/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/dashboard.js': [
            '<%= pkg.sources %>hbs/dashboard/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/analysis-reports.js': [
            '<%= pkg.sources %>hbs/analysis-reports/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/nav.js': [
            '<%= pkg.sources %>hbs/nav/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/widgets.js': [
            '<%= pkg.sources %>hbs/widgets/**/*.hbs'
          ]
          '<%= grunt.option("assetsDir") || pkg.assets %>js/templates/workspace.js': [
            '<%= pkg.sources %>hbs/workspace/**/*.hbs'
          ]


    clean:
      options:
        force: true
      css: ['<%= grunt.option("assetsDir") || pkg.assets %>css/']
      js: ['<%= grunt.option("assetsDir") || pkg.assets %>js/']
      build: ['<%= grunt.option("assetsDir") || pkg.assets %>build/']


    copy:
      js:
        expand: true, cwd: '<%= pkg.sources %>js/', src: ['**'], dest: '<%= grunt.option("assetsDir") || pkg.assets %>js/'
      build:
        expand: true, cwd: '<%= grunt.option("assetsDir") || pkg.assets %>build/js/', src: ['**'], dest: '<%= grunt.option("assetsDir") || pkg.assets %>js/'
      requirejs:
        src: '<%= pkg.sources %>js/require.js', dest: '<%= grunt.option("assetsDir") || pkg.assets %>js/require.js'


    express:
      test:
        options:
          script: 'src/test/server.js'
          port: expressPort
      testCoverage:
        options:
          script: 'src/test/server-coverage.js'
          port: expressPort
      dev:
        options:
          background: false
          script: 'src/test/server.js'


    casper:
      test:
        options:
          test: true
          'no-colors': true
          'fail-fast': true
          verbose: true
          'log-level': 'debug'
          parallel: !isWindows
          port: expressPort
        src: ['src/test/js/**/*.js']
      testCoverage:
        options:
          test: true
          'no-colors': true
          'fail-fast': true
          verbose: true
          'log-level': 'debug'
          parallel: !isWindows
          port: expressPort
        src: ['src/test/js/**/*.js']
      testCoverageLight:
        options:
          test: true
          'fail-fast': true
          verbose: true
          port: expressPort
        src: ['src/test/js/**/*<%= grunt.option("spec") %>*.js']
      single:
        options:
          test: true
          verbose: true
          'fail-fast': true
          port: expressPort
        src: ['src/test/js/<%= grunt.option("spec") %>-spec.js']
      testfile:
        options:
          test: true
          verbose: true
          'fail-fast': true
          port: expressPort
        src: ['<%= grunt.option("file") %>']


    uglify_parallel:
      build:
        files: [
          expand: true
          cwd: '<%= grunt.option("assetsDir") || pkg.assets %>js'
          src: ['**/*.js']
          dest: '<%= grunt.option("assetsDir") || pkg.assets %>js'
          ext: '.js'
        ]


    curl:
      resetCoverage:
        src:
          url: 'http://localhost:' + expressPort + '/coverage/reset'
          method: 'POST'
        dest: 'target/reset_coverage.dump'

      downloadCoverage:
        src: 'http://localhost:' + expressPort + '/coverage/download'
        dest: 'target/coverage.zip'


    unzip:
      'target/js-coverage': 'target/coverage.zip'


    replace:
      lcov:
        src: 'target/js-coverage/lcov.info'
        dest: 'target/js-coverage/lcov.info'
        replacements: [{
          from: '/webapp'
          to: ''
        }]


    jshint:
      dev:
        src: [
          '<%= pkg.sources %>js/**/*.js'
          '!<%= pkg.sources %>js/third-party/underscore.js'
          '!<%= pkg.sources %>js/third-party/**/*.js'
          '!<%= pkg.sources %>js/tests/**/*.js'
          '!<%= pkg.sources %>js/require.js'
        ]
        options:
          jshintrc: true


    watch:
      options:
        spawn: false

      less:
        files: '<%= pkg.sources %>less/**/*.less'
        tasks: ['less:dev']

      coffee:
        files: '<%= pkg.sources %>coffee/**/*.coffee'
        tasks: ['coffee:build', 'copy:js', 'concat:dev']

      js:
        files: '<%= pkg.sources %>js/**/*.js'
        tasks: ['copy:js', 'concat:dev']

      handlebars:
        files: '<%= pkg.sources %>hbs/**/*.hbs'
        tasks: ['handlebars:build']



  # Define tasks
  grunt.registerTask 'dev',
      ['clean:css', 'clean:js', 'less:dev', 'coffee:build', 'handlebars:build', 'copy:js', 'concat:dev']

  grunt.registerTask 'build',
      ['clean:css', 'clean:js', 'less:build', 'cssUrlRewrite:build', 'coffee:build', 'handlebars:build', 'copy:js',
       'concat:build', 'requirejs', 'clean:js', 'copy:build', 'copy:requirejs', 'uglify_parallel:build', 'clean:build']

  grunt.registerTask 'default',
      ['build']

  grunt.registerTask 'dw',
      ['dev', 'watch']

  grunt.registerTask 'test',
      ['dev', 'express:test', 'casper:test']

  grunt.registerTask 'testCoverage',
      ['dev', 'express:testCoverage', 'curl:resetCoverage', 'casper:testCoverage', 'curl:downloadCoverage', 'unzip', 'replace:lcov']

  grunt.registerTask 'testCoverageLight',
      ['dev', 'express:testCoverage', 'curl:resetCoverage', 'casper:testCoverageLight', 'curl:downloadCoverage', 'unzip', 'replace:lcov']

  grunt.registerTask 'single',
      ['dev', 'express:test', 'casper:single']

  grunt.registerTask 'testfile',
      ['dev', 'express:test', 'casper:testfile']

  # tasks used by Maven build (see pom.xml)
  grunt.registerTask 'maven-build-skip-tests-true-nocoverage',
      ['build']

  grunt.registerTask 'maven-build-skip-tests-false-nocoverage',
      ['test', 'build']

  grunt.registerTask 'maven-build-skip-tests-false-coverage',
      ['testCoverage', 'build']
