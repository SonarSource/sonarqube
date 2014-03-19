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
      dev:
        files: [
          expand: true
          cwd: '<%= pkg.assets %>coffee'
          src: ['**/*.coffee']
          dest: '<%= pkg.assets %>javascripts'
          ext: '.js'
        ]


    uglify:
      dev:
        options:
          beautify: true
        files:
          '<%= pkg.assets %>js/sonar.js': [
            '<%= pkg.assets %>javascripts/third-party/jquery.js'
            '<%= pkg.assets %>javascripts/third-party/jquery-ui.js'
            '<%= pkg.assets %>javascripts/third-party/d3.js'
            '<%= pkg.assets %>javascripts/third-party/underscore.js'
            '<%= pkg.assets %>javascripts/third-party/select2.js'
            '<%= pkg.assets %>javascripts/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>javascripts/translate.js'
            '<%= pkg.assets %>javascripts/widgets/widget.js'
            '<%= pkg.assets %>javascripts/widgets/bubble-chart.js'
            '<%= pkg.assets %>javascripts/widgets/timeline.js'
            '<%= pkg.assets %>javascripts/widgets/stack-area.js'
            '<%= pkg.assets %>javascripts/widgets/pie-chart.js'
            '<%= pkg.assets %>javascripts/widgets/histogram.js'
            '<%= pkg.assets %>javascripts/top-search.js'
            '<%= pkg.assets %>javascripts/sortable.js'
            '<%= pkg.assets %>javascripts/common/inputs.js'
            '<%= pkg.assets %>javascripts/application.js'
            '<%= pkg.assets %>javascripts/dashboard.js'
            '<%= pkg.assets %>javascripts/duplication.js'
            '<%= pkg.assets %>javascripts/resource.js'
            '<%= pkg.assets %>javascripts/issue.js'
            '<%= pkg.assets %>javascripts/recent-history.js'
          ]
      build:
        options:
          preserveComments: false # remove all comments
        files:
          '<%= pkg.assets %>build/js/sonar.js': [
            '<%= pkg.assets %>javascripts/third-party/jquery.js'
            '<%= pkg.assets %>javascripts/third-party/jquery-ui.js'
            '<%= pkg.assets %>javascripts/third-party/d3.js'
            '<%= pkg.assets %>javascripts/third-party/underscore.js'
            '<%= pkg.assets %>javascripts/third-party/select2.js'
            '<%= pkg.assets %>javascripts/select2-jquery-ui-fix.js'
            '<%= pkg.assets %>javascripts/translate.js'
            '<%= pkg.assets %>javascripts/widgets/widget.js'
            '<%= pkg.assets %>javascripts/widgets/bubble-chart.js'
            '<%= pkg.assets %>javascripts/widgets/timeline.js'
            '<%= pkg.assets %>javascripts/widgets/stack-area.js'
            '<%= pkg.assets %>javascripts/widgets/pie-chart.js'
            '<%= pkg.assets %>javascripts/widgets/histogram.js'
            '<%= pkg.assets %>javascripts/top-search.js'
            '<%= pkg.assets %>javascripts/sortable.js'
            '<%= pkg.assets %>javascripts/common/inputs.js'
            '<%= pkg.assets %>javascripts/application.js'
            '<%= pkg.assets %>javascripts/dashboard.js'
            '<%= pkg.assets %>javascripts/duplication.js'
            '<%= pkg.assets %>javascripts/resource.js'
            '<%= pkg.assets %>javascripts/issue.js'
            '<%= pkg.assets %>javascripts/recent-history.js'
          ]


    requirejs:
      options:
        baseUrl: '<%= pkg.assets %>javascripts'

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


    watch:
      options:
        spawn: false

      less:
        files: '<%= pkg.assets %>less/**/*.less'
        tasks: ['less:dev']

      coffee:
        files: '<%= pkg.assets %>coffee/**/*.coffee'
        tasks: ['coffee:dev']

      uglify:
        files: '<%= pkg.assets %>javascripts/**/*.js'
        tasks: ['uglify:dev']



  # Load grunt-contrib-* plugins
  grunt.loadNpmTasks 'grunt-contrib-less'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-uglify'
  grunt.loadNpmTasks 'grunt-contrib-requirejs'
  grunt.loadNpmTasks 'grunt-contrib-watch'


  # Define tasks
  grunt.registerTask 'default', ['less:dev', 'coffee:dev', 'uglify:dev']
  grunt.registerTask 'build', ['less:build', 'coffee:dev', 'uglify:build', 'requirejs']
