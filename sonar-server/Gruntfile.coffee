module.exports = (grunt) ->
  grunt.loadNpmTasks('grunt-karma')
  pkg = grunt.file.readJSON('package.json')

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
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/select2.js'
            '<%= pkg.assets %>js/third-party/keymaster.js'
            '<%= pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>js/widgets/base.js'
            '<%= pkg.assets %>js/widgets/widget.js'
            '<%= pkg.assets %>js/widgets/bubble-chart.js'
            '<%= pkg.assets %>js/widgets/timeline.js'
            '<%= pkg.assets %>js/widgets/stack-area.js'
            '<%= pkg.assets %>js/widgets/pie-chart.js'
            '<%= pkg.assets %>js/widgets/histogram.js'
            '<%= pkg.assets %>js/widgets/word-cloud.js'
            '<%= pkg.assets %>js/top-search.js'
            '<%= pkg.assets %>js/sortable.js'
            '<%= pkg.assets %>js/common/inputs.js'
            '<%= pkg.assets %>js/common/dialogs.js'
            '<%= pkg.assets %>js/application.js'
            '<%= pkg.assets %>js/dashboard.js'
            '<%= pkg.assets %>js/duplication.js'
            '<%= pkg.assets %>js/resource.js'
            '<%= pkg.assets %>js/issue.js'
            '<%= pkg.assets %>js/recent-history.js'
            '<%= pkg.assets %>js/latinize.js'
          ]
      build:
        files:
          '<%= pkg.assets %>build/js/sonar.js': [
            '<%= pkg.assets %>js/translate.js'
            '<%= pkg.assets %>js/third-party/jquery.js'
            '<%= pkg.assets %>js/third-party/jquery-ui.js'
            '<%= pkg.assets %>js/third-party/d3.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/select2.js'
            '<%= pkg.assets %>js/third-party/keymaster.js'
            '<%= pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>js/widgets/base.js'
            '<%= pkg.assets %>js/widgets/widget.js'
            '<%= pkg.assets %>js/widgets/bubble-chart.js'
            '<%= pkg.assets %>js/widgets/timeline.js'
            '<%= pkg.assets %>js/widgets/stack-area.js'
            '<%= pkg.assets %>js/widgets/pie-chart.js'
            '<%= pkg.assets %>js/widgets/histogram.js'
            '<%= pkg.assets %>js/widgets/word-cloud.js'
            '<%= pkg.assets %>js/top-search.js'
            '<%= pkg.assets %>js/sortable.js'
            '<%= pkg.assets %>js/common/inputs.js'
            '<%= pkg.assets %>js/common/dialogs.js'
            '<%= pkg.assets %>js/application.js'
            '<%= pkg.assets %>js/dashboard.js'
            '<%= pkg.assets %>js/duplication.js'
            '<%= pkg.assets %>js/resource.js'
            '<%= pkg.assets %>js/issue.js'
            '<%= pkg.assets %>js/recent-history.js'
            '<%= pkg.assets %>js/latinize.js'
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

      issues: options:
        name: 'issues/app'
        out: '<%= pkg.assets %>build/js/issues/app.js'

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

      dashboardFile: options:
        name: 'dashboard/file-app'
        out: '<%= pkg.assets %>build/js/dashboard/file-app.js'

      componentViewer: options:
        name: 'component-viewer/app'
        out: '<%= pkg.assets %>build/js/component-viewer/app.js'


    handlebars:
      options:
        amd: true
        namespace: 'SS.Templates'
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
          '<%= pkg.assets %>js/templates/coding-rules.js': [
            '<%= pkg.sources %>hbs/common/**/*.hbs'
            '<%= pkg.sources %>hbs/coding-rules/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/quality-gates.js': [
            '<%= pkg.sources %>hbs/quality-gates/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/component-viewer.js': [
            '<%= pkg.sources %>hbs/component-viewer/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/issues.js': [
            '<%= pkg.sources %>hbs/common/**/*.hbs'
            '<%= pkg.sources %>hbs/issues/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/api-documentation.js': [
            '<%= pkg.sources %>hbs/api-documentation/**/*.hbs'
          ]


    clean:
      options: {
        force: true
      },
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


    karma:
      unit:
        configFile: 'karma.conf.js'
        singleRun: true
        logLevel: 'DEBUG'


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


  # Define tasks
  grunt.registerTask 'dev', ['clean:css', 'clean:js',
                             'less:dev',
                             'coffee:build', 'handlebars:build', 'copy:js',
                             'concat:dev']


  grunt.registerTask 'default', ['clean:css', 'clean:js',
                                 'less:build', 'cssUrlRewrite:build'
                                 'coffee:build', 'handlebars:build', 'copy:js',
                                 'concat:build',
                                 'requirejs', 'clean:js', 'copy:build', 'copy:requirejs', 'clean:build']

  grunt.registerTask 'test', ['coffee:build', 'handlebars:build', 'copy:js', 'concat:dev', 'karma:unit']
