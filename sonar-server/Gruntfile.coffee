module.exports = (grunt) ->
  grunt.initConfig
    pkg: grunt.file.readJSON('package.json')

    less:
      dev:
        files:
          '<%= pkg.assets %>css/sonar.css': [
            '<%= pkg.assets %>less/yui-reset-font.less'
            '<%= pkg.assets %>less/jquery-ui.less'
            '<%= pkg.assets %>less/select2.less'
            '<%= pkg.assets %>less/select2-sonar.less'
            '<%= pkg.assets %>less/layout.less'
            '<%= pkg.assets %>less/style.less'
            '<%= pkg.assets %>less/icons.less'
            '<%= pkg.assets %>less/ui.less'
            '<%= pkg.assets %>less/sonar-colorizer.less'
            '<%= pkg.assets %>less/dashboard.less'
            '<%= pkg.assets %>less/select-list.less'
            '<%= pkg.assets %>less/navigator.less'
            '<%= pkg.assets %>less/*.less'
          ]
      build:
        options:
          cleancss: true
        files:
          '<%= pkg.assets %>build/css/sonar.css': [
            '<%= pkg.assets %>less/yui-reset-font.less'
            '<%= pkg.assets %>less/jquery-ui.less'
            '<%= pkg.assets %>less/select2.less'
            '<%= pkg.assets %>less/select2-sonar.less'
            '<%= pkg.assets %>less/layout.less'
            '<%= pkg.assets %>less/style.less'
            '<%= pkg.assets %>less/icons.less'
            '<%= pkg.assets %>less/ui.less'
            '<%= pkg.assets %>less/sonar-colorizer.less'
            '<%= pkg.assets %>less/dashboard.less'
            '<%= pkg.assets %>less/select-list.less'
            '<%= pkg.assets %>less/navigator.less'
            '<%= pkg.assets %>less/*.less'
          ]


    coffee:
      build:
        files: [
          expand: true
          cwd: '<%= pkg.assets %>coffee'
          src: ['**/*.coffee']
          dest: '<%= pkg.assets %>js'
          ext: '.js'
        ]


    uglify:
      dev:
        options:
          beautify: true
        files:
          '<%= pkg.assets %>js/sonar.js': [
            '<%= pkg.assets %>js/third-party/jquery.js'
            '<%= pkg.assets %>js/third-party/jquery-ui.js'
            '<%= pkg.assets %>js/third-party/d3.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/select2.js'
            '<%= pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>js/translate.js'
            '<%= pkg.assets %>js/widgets/widget.js'
            '<%= pkg.assets %>js/widgets/bubble-chart.js'
            '<%= pkg.assets %>js/widgets/timeline.js'
            '<%= pkg.assets %>js/widgets/stack-area.js'
            '<%= pkg.assets %>js/widgets/pie-chart.js'
            '<%= pkg.assets %>js/widgets/histogram.js'
            '<%= pkg.assets %>js/top-search.js'
            '<%= pkg.assets %>js/sortable.js'
            '<%= pkg.assets %>js/common/inputs.js'
            '<%= pkg.assets %>js/application.js'
            '<%= pkg.assets %>js/dashboard.js'
            '<%= pkg.assets %>js/duplication.js'
            '<%= pkg.assets %>js/resource.js'
            '<%= pkg.assets %>js/issue.js'
            '<%= pkg.assets %>js/recent-history.js'
          ]
      build:
        options:
          preserveComments: false # remove all comments
        files:
          '<%= pkg.assets %>build/js/sonar.js': [
            '<%= pkg.assets %>js/third-party/jquery.js'
            '<%= pkg.assets %>js/third-party/jquery-ui.js'
            '<%= pkg.assets %>js/third-party/d3.js'
            '<%= pkg.assets %>js/third-party/underscore.js'
            '<%= pkg.assets %>js/third-party/select2.js'
            '<%= pkg.assets %>js/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>js/translate.js'
            '<%= pkg.assets %>js/widgets/widget.js'
            '<%= pkg.assets %>js/widgets/bubble-chart.js'
            '<%= pkg.assets %>js/widgets/timeline.js'
            '<%= pkg.assets %>js/widgets/stack-area.js'
            '<%= pkg.assets %>js/widgets/pie-chart.js'
            '<%= pkg.assets %>js/widgets/histogram.js'
            '<%= pkg.assets %>js/top-search.js'
            '<%= pkg.assets %>js/sortable.js'
            '<%= pkg.assets %>js/common/inputs.js'
            '<%= pkg.assets %>js/application.js'
            '<%= pkg.assets %>js/dashboard.js'
            '<%= pkg.assets %>js/duplication.js'
            '<%= pkg.assets %>js/resource.js'
            '<%= pkg.assets %>js/issue.js'
            '<%= pkg.assets %>js/recent-history.js'
          ]


    requirejs:
      options:
        baseUrl: '<%= pkg.assets %>js'
        preserveLicenseComments: false,

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
            '<%= pkg.assets %>templates/common/**/*.hbs'
            '<%= pkg.assets %>templates/coding-rules/**/*.hbs'
          ]
          '<%= pkg.assets %>js/templates/quality-gates.js': [
            '<%= pkg.assets %>templates/quality-gates/**/*.hbs'
          ]


    watch:
      options:
        spawn: false

      less:
        files: '<%= pkg.assets %>less/**/*.less'
        tasks: ['less:dev']

      coffee:
        files: '<%= pkg.assets %>coffee/**/*.coffee'
        tasks: ['coffee:build']

      uglify:
        files: '<%= pkg.assets %>js/**/*.js'
        tasks: ['uglify:dev']

      handlebars:
        files: '<%= pkg.assets %>templates/**/*.hbs'
        tasks: ['handlebars:build']



  # Load grunt-contrib-* plugins
  grunt.loadNpmTasks 'grunt-contrib-less'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-uglify'
  grunt.loadNpmTasks 'grunt-contrib-requirejs'
  grunt.loadNpmTasks 'grunt-contrib-handlebars'
  grunt.loadNpmTasks 'grunt-contrib-watch'


  # Define tasks
  grunt.registerTask 'dev', ['less:dev', 'coffee:build', 'uglify:dev', 'handlebars:build']
  grunt.registerTask 'default', ['less:build', 'coffee:build', 'uglify:build', 'handlebars:build', 'requirejs']
