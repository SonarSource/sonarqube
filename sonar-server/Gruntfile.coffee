module.exports = (grunt) ->
  grunt.initConfig
    pkg: grunt.file.readJSON('package.json')

    less:
      dev:
        files:
          '<%= pkg.assets %>stylesheets/sonar.css': [
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


    watch:
      options:
        spawn: false

      less:
        files: '<%= pkg.assets %>less/**/*.less'
        tasks: ['less:dev']

      coffee:
        files: '<%= pkg.assets %>coffee/**/*.coffee'
        tasks: ['coffee:dev']


  # Load grunt-contrib-* plugins
  grunt.loadNpmTasks 'grunt-contrib-less'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-watch'


  # Define tasks
  grunt.registerTask 'default', ['less:dev', 'coffee:dev']
