module.exports = (grunt) ->
  pkg = grunt.file.readJSON('package.json')
  expressPort = '<%= grunt.option("port") || 3000 %>'

  grunt.initConfig
    pkg: grunt.file.readJSON('package.json')

    less:
      dev:
        files:
          '<%= pkg.assets %>css/sonar.css': [
            '<%= pkg.sources %>less/yui-reset-font.less'
            '<%= pkg.sources %>less/jquery-ui.less'
            '<%= pkg.sources %>less/select2.less'
            '<%= pkg.sources %>less/select2-sonar.less'
            '<%= pkg.sources %>less/layout.less'
            '<%= pkg.sources %>less/style.less'
            '<%= pkg.sources %>less/icons.less'
            '<%= pkg.sources %>less/ui.less'
            '<%= pkg.sources %>less/sonar-colorizer.less'
            '<%= pkg.sources %>less/dashboard.less'
            '<%= pkg.sources %>less/select-list.less'
            '<%= pkg.sources %>less/navigator.less'
            '<%= pkg.sources %>less/*.less'
          ]
      build:
        options:
          cleancss: true
        files:
          '<%= pkg.assets %>css/sonar.css': [
            '<%= pkg.sources %>less/yui-reset-font.less'
            '<%= pkg.sources %>less/jquery-ui.less'
            '<%= pkg.sources %>less/select2.less'
            '<%= pkg.sources %>less/select2-sonar.less'
            '<%= pkg.sources %>less/layout.less'
            '<%= pkg.sources %>less/style.less'
            '<%= pkg.sources %>less/icons.less'
            '<%= pkg.sources %>less/ui.less'
            '<%= pkg.sources %>less/sonar-colorizer.less'
            '<%= pkg.sources %>less/dashboard.less'
            '<%= pkg.sources %>less/select-list.less'
            '<%= pkg.sources %>less/navigator.less'
            '<%= pkg.sources %>less/*.less'
          ]


    cssUrlRewrite:
      build:
        src: '<%= pkg.assets %>css/sonar.css'
        dest: '<%= pkg.assets %>css/sonar.css'
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
          dest: '<%= pkg.assets %>js'
          ext: '.js'
        ]


    concat:
      dev:
        files:
          '<%= pkg.assets %>js/sonar.js': [
            '<%= pkg.assets %>js/translate.js'
            '<%= pkg.assets %>js/third-party/jquery.js'
            '<%= pkg.assets %>js/third-party/jquery-ui.js'
            '<%= pkg.assets %>js/third-party/d3.js'
            '<%= pkg.assets %>js/third-party/latinize.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/backbone.js'
            '<%= pkg.assets %>js/third-party/backbone.marionette.js'
            '<%= pkg.assets %>js/third-party/handlebars.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/select2.js'
            '<%= pkg.assets %>js/third-party/keymaster.js'
            '<%= pkg.assets %>js/third-party/moment.js'
            '<%= pkg.assets %>js/third-party/numeral.js'
            '<%= pkg.assets %>js/third-party/numeral-languages.js'
            '<%= pkg.assets %>js/third-party/bootstrap/tooltip.js'
            '<%= pkg.assets %>js/third-party/bootstrap/dropdown.js'
            '<%= pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>js/widgets/base.js'
            '<%= pkg.assets %>js/widgets/widget.js'
            '<%= pkg.assets %>js/widgets/bubble-chart.js'
            '<%= pkg.assets %>js/widgets/timeline.js'
            '<%= pkg.assets %>js/widgets/stack-area.js'
            '<%= pkg.assets %>js/widgets/pie-chart.js'
            '<%= pkg.assets %>js/widgets/histogram.js'
            '<%= pkg.assets %>js/widgets/word-cloud.js'
            '<%= pkg.assets %>js/widgets/tag-cloud.js'
            '<%= pkg.assets %>js/widgets/treemap.js'
            '<%= pkg.assets %>js/graphics/pie-chart.js'
            '<%= pkg.assets %>js/top-search.js'
            '<%= pkg.assets %>js/sortable.js'
            '<%= pkg.assets %>js/common/inputs.js'
            '<%= pkg.assets %>js/common/dialogs.js'
            '<%= pkg.assets %>js/common/processes.js'
            '<%= pkg.assets %>js/common/jquery-isolated-scroll.js'
            '<%= pkg.assets %>js/common/handlebars-extensions.js'
            '<%= pkg.assets %>js/application.js'
            '<%= pkg.assets %>js/csv.js'
            '<%= pkg.assets %>js/dashboard.js'
            '<%= pkg.assets %>js/recent-history.js'
          ]
      build:
        files:
          '<%= pkg.assets %>build/js/sonar.js': [
            '<%= pkg.assets %>js/translate.js'
            '<%= pkg.assets %>js/third-party/jquery.js'
            '<%= pkg.assets %>js/third-party/jquery-ui.js'
            '<%= pkg.assets %>js/third-party/d3.js'
            '<%= pkg.assets %>js/third-party/latinize.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/backbone.js'
            '<%= pkg.assets %>js/third-party/backbone.marionette.js'
            '<%= pkg.assets %>js/third-party/handlebars.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/select2.js'
            '<%= pkg.assets %>js/third-party/keymaster.js'
            '<%= pkg.assets %>js/third-party/moment.js'
            '<%= pkg.assets %>js/third-party/numeral.js'
            '<%= pkg.assets %>js/third-party/numeral-languages.js'
            '<%= pkg.assets %>js/third-party/bootstrap/tooltip.js'
            '<%= pkg.assets %>js/third-party/bootstrap/dropdown.js'
            '<%= pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>js/widgets/base.js'
            '<%= pkg.assets %>js/widgets/widget.js'
            '<%= pkg.assets %>js/widgets/bubble-chart.js'
            '<%= pkg.assets %>js/widgets/timeline.js'
            '<%= pkg.assets %>js/widgets/stack-area.js'
            '<%= pkg.assets %>js/widgets/pie-chart.js'
            '<%= pkg.assets %>js/widgets/histogram.js'
            '<%= pkg.assets %>js/widgets/word-cloud.js'
            '<%= pkg.assets %>js/widgets/tag-cloud.js'
            '<%= pkg.assets %>js/widgets/treemap.js'
            '<%= pkg.assets %>js/graphics/pie-chart.js'
            '<%= pkg.assets %>js/top-search.js'
            '<%= pkg.assets %>js/sortable.js'
            '<%= pkg.assets %>js/common/inputs.js'
            '<%= pkg.assets %>js/common/dialogs.js'
            '<%= pkg.assets %>js/common/processes.js'
            '<%= pkg.assets %>js/common/jquery-isolated-scroll.js'
            '<%= pkg.assets %>js/common/handlebars-extensions.js'
            '<%= pkg.assets %>js/application.js'
            '<%= pkg.assets %>js/csv.js'
            '<%= pkg.assets %>js/dashboard.js'
            '<%= pkg.assets %>js/recent-history.js'
          ]


    requirejs:
      options:
        baseUrl: '<%= pkg.assets %>js/'
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
        out: '<%= pkg.assets %>build/js/quality-gate/app.js'

      codingRules: options:
        name: 'coding-rules/app'
        out: '<%= pkg.assets %>build/js/coding-rules/app.js'

      codingRulesShow: options:
        name: 'coding-rules/show-app'
        out: '<%= pkg.assets %>build/js/coding-rules/show-app.js'

      issues: options:
        name: 'issues/app-new'
        out: '<%= pkg.assets %>build/js/issues/app-new.js'

      measures: options:
        name: 'measures/app'
        out: '<%= pkg.assets %>build/js/measures/app.js'

      selectList: options:
        name: 'common/select-list'
        out: '<%= pkg.assets %>build/js/common/select-list.js'

      apiDocumentation: options:
        name: 'api-documentation/app'
        out: '<%= pkg.assets %>build/js/api-documentation/app.js'

      drilldown: options:
        name: 'drilldown/app'
        out: '<%= pkg.assets %>build/js/drilldown/app.js'

      dashboard: options:
        name: 'dashboard/app'
        out: '<%= pkg.assets %>build/js/dashboard/app.js'

      sourceViewer: options:
        name: 'source-viewer/app'
        out: '<%= pkg.assets %>build/js/source-viewer/app.js'

      design: options:
        name: 'design/app'
        out: '<%= pkg.assets %>build/js/design/app.js'

      libraries: options:
        name: 'libraries/app'
        out: '<%= pkg.assets %>build/js/libraries/app.js'

      monitoring: options:
        name: 'analysis-reports/app'
        out: '<%= pkg.assets %>build/js/analysis-reports/app.js'

      nav: options:
        name: 'nav/app'
        out: '<%= pkg.assets %>build/js/nav/app.js'


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
          '<%= pkg.assets %>js/templates/navigator.js': [
            '<%= pkg.sources %>hbs/navigator/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/coding-rules.js': [
            '<%= pkg.sources %>hbs/common/**/*.hbs'
            '<%= pkg.sources %>hbs/coding-rules/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/quality-gates.js': [
            '<%= pkg.sources %>hbs/quality-gates/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/source-viewer.js': [
            '<%= pkg.sources %>hbs/source-viewer/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/issue.js': [
            '<%= pkg.sources %>hbs/common/**/*.hbs'
            '<%= pkg.sources %>hbs/issue/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/issues.js': [
            '<%= pkg.sources %>hbs/issues/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/api-documentation.js': [
            '<%= pkg.sources %>hbs/api-documentation/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/design.js': [
            '<%= pkg.sources %>hbs/design/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/libraries.js': [
            '<%= pkg.sources %>hbs/libraries/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/dashboard.js': [
            '<%= pkg.sources %>hbs/dashboard/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/analysis-reports.js': [
            '<%= pkg.sources %>hbs/analysis-reports/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/nav.js': [
            '<%= pkg.sources %>hbs/nav/**/*.hbs'
          ]


    clean:
      options:
        force: true
      css: ['<%= pkg.assets %>css/']
      js: ['<%= pkg.assets %>js/']
      build: ['<%= pkg.assets %>build/']


    copy:
      js:
        expand: true, cwd: '<%= pkg.sources %>js/', src: ['**'], dest: '<%= pkg.assets %>js/'
      build:
        expand: true, cwd: '<%= pkg.assets %>build/js/', src: ['**'], dest: '<%= pkg.assets %>js/'
      requirejs:
        src: '<%= pkg.sources %>js/require.js', dest: '<%= pkg.assets %>js/require.js'


    express:
      test:
        options:
          script: '<%= pkg.sources %>js/tests/e2e/server.js'
          port: expressPort
      dev:
        options:
          background: false
          script: '<%= pkg.sources %>js/tests/e2e/server.js'


    casper:
      test:
        options:
          test: true
          'no-colors': true
          'fail-fast': true
          concise: true
          port: expressPort
        src: ['<%= pkg.sources %>js/tests/e2e/tests/**/*.js']
      single:
        options:
          test: true
          verbose: true
          'fail-fast': true
          port: expressPort
        src: ['<%= pkg.sources %>js/tests/e2e/tests/<%= grunt.option("spec") %>-spec.js']
      testfile:
        options:
          test: true
          verbose: true
          'fail-fast': true
          port: expressPort
        src: ['<%= grunt.option("file") %>']


    uglify:
      build:
        files: [
          expand: true
          cwd: '<%= pkg.assets %>js'
          src: ['**/*.js']
          dest: '<%= pkg.assets %>js'
          ext: '.js'
        ]


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



  # Load grunt-contrib-* plugins
  grunt.loadNpmTasks 'grunt-contrib-less'
  grunt.loadNpmTasks 'grunt-css-url-rewrite'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-uglify'
  grunt.loadNpmTasks 'grunt-contrib-requirejs'
  grunt.loadNpmTasks 'grunt-contrib-handlebars'
  grunt.loadNpmTasks 'grunt-contrib-watch'
  grunt.loadNpmTasks 'grunt-contrib-clean'
  grunt.loadNpmTasks 'grunt-contrib-copy'
  grunt.loadNpmTasks 'grunt-contrib-concat'
  grunt.loadNpmTasks 'grunt-contrib-jshint'
  grunt.loadNpmTasks 'grunt-express-server'
  grunt.loadNpmTasks 'grunt-casper'


  # Define tasks
  grunt.registerTask 'dev',
      ['clean:css', 'clean:js', 'less:dev', 'coffee:build', 'handlebars:build', 'copy:js', 'concat:dev']

  grunt.registerTask 'build',
      ['clean:css', 'clean:js', 'less:build', 'cssUrlRewrite:build', 'coffee:build', 'handlebars:build', 'copy:js',
       'concat:build', 'requirejs', 'clean:js', 'copy:build', 'copy:requirejs', 'uglify:build', 'clean:build']

  grunt.registerTask 'default',
      ['build']

  grunt.registerTask 'test',
      ['dev', 'express:test', 'casper:test']

  grunt.registerTask 'single',
      ['dev', 'express:test', 'casper:single']

  grunt.registerTask 'testfile',
      ['dev', 'express:test', 'casper:testfile']

  # tasks used by Maven build (see pom.xml)
  grunt.registerTask 'maven-build-skip-tests-true',
      ['build']

  grunt.registerTask 'maven-build-skip-tests-false',
      ['test', 'build']
