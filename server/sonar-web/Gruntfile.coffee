module.exports = (grunt) ->
  require('jit-grunt')(grunt, {
    unzip: 'grunt-zip'
    replace: 'grunt-text-replace'
  });
  require('time-grunt')(grunt);

  useBrowserStack = !!process.env['BROWSERSTACK_USERNAME'] && !!process.env['BROWSERSTACK_ACCESS_KEY']
  expressPort = '<%= grunt.option("port") || 3000 %>'
  internPort = '<%= grunt.option("internPort") || 9100 %>'

  grunt.initConfig
    pkg: grunt.file.readJSON('package.json')

    SOURCE_PATH: './src/main'
    ASSETS_PATH: grunt.option("assetsDir") || './src/main/webapp'
    BUILD_PATH: './build'

    less:
      build:
        options:
          cleancss: true
        files:
          '<%= BUILD_PATH %>/css/sonar.css': [
            '<%= SOURCE_PATH %>/less/jquery-ui.less'
            '<%= SOURCE_PATH %>/less/select2.less'
            '<%= SOURCE_PATH %>/less/select2-sonar.less'

            '<%= SOURCE_PATH %>/less/init.less'
            '<%= SOURCE_PATH %>/less/components.less'
            '<%= SOURCE_PATH %>/less/pages.less'

            '<%= SOURCE_PATH %>/less/style.less'

            '<%= SOURCE_PATH %>/less/*.less'
          ]


    babel:
      build:
        options:
          modules: 'amd'
        files: [
          expand: true
          cwd: '<%= SOURCE_PATH %>/js'
          src: [
            '**/*.jsx'
            '**/api/**/*.js'
            '**/apps/**/*.js'
          ]
          dest: '<%= BUILD_PATH %>/js'
          ext: '.js'
        ]


    concat:
      build:
        files:
          '<%= BUILD_PATH %>/js/sonar.js': [
            '<%= BUILD_PATH %>/js/libs/translate.js'
            '<%= BUILD_PATH %>/js/libs/third-party/jquery.js'
            '<%= BUILD_PATH %>/js/libs/third-party/jquery-ui.js'
            '<%= BUILD_PATH %>/js/libs/third-party/d3.js'
            '<%= BUILD_PATH %>/js/libs/third-party/latinize.js'
            '<%= BUILD_PATH %>/js/libs/third-party/underscore.js'
            '<%= BUILD_PATH %>/js/libs/third-party/backbone.js'
            '<%= BUILD_PATH %>/js/libs/third-party/backbone-super.js'
            '<%= BUILD_PATH %>/js/libs/third-party/backbone.marionette.js'
            '<%= BUILD_PATH %>/js/libs/third-party/handlebars.js'
            '<%= BUILD_PATH %>/js/libs/third-party/select2.js'
            '<%= BUILD_PATH %>/js/libs/third-party/keymaster.js'
            '<%= BUILD_PATH %>/js/libs/third-party/moment.js'
            '<%= BUILD_PATH %>/js/libs/third-party/numeral.js'
            '<%= BUILD_PATH %>/js/libs/third-party/numeral-languages.js'
            '<%= BUILD_PATH %>/js/libs/third-party/bootstrap/tooltip.js'
            '<%= BUILD_PATH %>/js/libs/third-party/bootstrap/dropdown.js'
            '<%= BUILD_PATH %>/js/libs/third-party/md5.js'
            '<%= BUILD_PATH %>/js/libs/select2-jquery-ui-fix.js'

            '<%= BUILD_PATH %>/js/libs/widgets/base.js'
            '<%= BUILD_PATH %>/js/libs/widgets/widget.js'
            '<%= BUILD_PATH %>/js/libs/widgets/bubble-chart.js'
            '<%= BUILD_PATH %>/js/libs/widgets/timeline.js'
            '<%= BUILD_PATH %>/js/libs/widgets/stack-area.js'
            '<%= BUILD_PATH %>/js/libs/widgets/pie-chart.js'
            '<%= BUILD_PATH %>/js/libs/widgets/histogram.js'
            '<%= BUILD_PATH %>/js/libs/widgets/word-cloud.js'
            '<%= BUILD_PATH %>/js/libs/widgets/tag-cloud.js'
            '<%= BUILD_PATH %>/js/libs/widgets/treemap.js'

            '<%= BUILD_PATH %>/js/libs/graphics/pie-chart.js'
            '<%= BUILD_PATH %>/js/libs/graphics/barchart.js'
            '<%= BUILD_PATH %>/js/libs/sortable.js'

            '<%= BUILD_PATH %>/js/libs/inputs.js'
            '<%= BUILD_PATH %>/js/components/common/dialogs.js'
            '<%= BUILD_PATH %>/js/components/common/processes.js'
            '<%= BUILD_PATH %>/js/components/common/jquery-isolated-scroll.js'
            '<%= BUILD_PATH %>/js/components/common/handlebars-extensions.js'

            '<%= BUILD_PATH %>/js/libs/application.js'
            '<%= BUILD_PATH %>/js/libs/csv.js'
            '<%= BUILD_PATH %>/js/libs/dashboard.js'
            '<%= BUILD_PATH %>/js/libs/recent-history.js'
            '<%= BUILD_PATH %>/js/libs/third-party/require.js'
          ]


    requirejs:
      options:
        baseUrl: '<%= BUILD_PATH %>/js/'
        preserveLicenseComments: false
        paths:
          'react': 'libs/third-party/react-with-addons'
          'underscore': 'libs/third-party/shim/underscore-shim'
          'jquery': 'libs/third-party/shim/jquery-shim'
          'backbone': 'libs/third-party/shim/backbone-shim'
          'backbone.marionette': 'libs/third-party/shim/marionette-shim'

      issuesContext: options:
        name: 'apps/issues/app-context'
        out: '<%= ASSETS_PATH %>/js/apps/issues/app-context.js'

      selectList: options:
        name: 'components/common/select-list'
        out: '<%= ASSETS_PATH %>/js/components/common/select-list.js'

      app: options:
        name: 'apps/<%= grunt.option("app") %>/app'
        out: '<%= ASSETS_PATH %>/js/apps/<%= grunt.option("app") %>/app.js'

      widget: options:
        name: 'widgets/<%= grunt.option("widget") %>/widget'
        out: '<%= ASSETS_PATH %>/js/widgets/<%= grunt.option("widget") %>/widget.js'


    concurrent:
      build:
        tasks: [
          'uglify:build'
          # apps
          'build-app:account'
          'build-app:api-documentation'
          'build-app:coding-rules'
          'build-app:computation'
          'build-app:custom-measures'
          'build-app:drilldown'
          'build-app:global-permissions'
          'build-app:groups'
          'build-app:issues'
          'build-app:maintenance'
          'build-app:markdown'
          'build-app:measures'
          'build-app:metrics'
          'build-app:nav'
          'build-app:project-permissions'
          'build-app:provisioning'
          'build-app:quality-gates'
          'build-app:quality-profiles'
          'build-app:source-viewer'
          'build-app:users'
          'build-app:update-center'
          # widgets
          'build-widget:issue-filter'
          # other
          'requirejs:issuesContext'
          'requirejs:selectList'
        ]


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
          '<%= BUILD_PATH %>/js/components/navigator/templates.js': [
            '<%= SOURCE_PATH %>/js/components/navigator/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/coding-rules/templates.js': [
            '<%= SOURCE_PATH %>/js/components/common/templates/**/*.hbs'
            '<%= SOURCE_PATH %>/js/apps/coding-rules/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/quality-gates/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/quality-gates/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/quality-profiles/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/quality-profiles/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/components/source-viewer/templates.js': [
            '<%= SOURCE_PATH %>/js/components/source-viewer/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/components/issue/templates.js': [
            '<%= SOURCE_PATH %>/js/components/common/templates/**/*.hbs'
            '<%= SOURCE_PATH %>/js/components/issue/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/issues/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/issues/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/api-documentation/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/api-documentation/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/nav/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/nav/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/widgets/issue-filter/templates.js': [
            '<%= SOURCE_PATH %>/js/widgets/issue-filter/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/components/workspace/templates.js': [
            '<%= SOURCE_PATH %>/js/components/workspace/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/markdown/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/markdown/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/users/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/users/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/groups/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/groups/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/provisioning/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/provisioning/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/computation/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/computation/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/metrics/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/metrics/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/maintenance/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/maintenance/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/account/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/account/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/update-center/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/update-center/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/custom-measures/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/custom-measures/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/global-permissions/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/global-permissions/templates/**/*.hbs'
          ]
          '<%= BUILD_PATH %>/js/apps/project-permissions/templates.js': [
            '<%= SOURCE_PATH %>/js/apps/project-permissions/templates/**/*.hbs'
          ]


    clean:
      options:
        force: true
      css: ['<%= ASSETS_PATH %>/css']
      js: ['<%= ASSETS_PATH %>/js']
      build: ['<%= BUILD_PATH %>']


    copy:
      js:
        expand: true, cwd: '<%= SOURCE_PATH %>/js', src: ['**/*.js'], dest: '<%= BUILD_PATH %>/js'
      'assets-js':
        src: '<%= BUILD_PATH %>/js/sonar.js', dest: '<%= ASSETS_PATH %>/js/sonar.js'
      'assets-all-js':
        expand: true, cwd: '<%= BUILD_PATH %>/js', src: ['**/*.js'], dest: '<%= ASSETS_PATH %>/js'
      'assets-css':
        src: '<%= BUILD_PATH %>/css/sonar.css', dest: '<%= ASSETS_PATH %>/css/sonar.css'


    uglify:
      build:
        src: '<%= ASSETS_PATH %>/js/sonar.js'
        dest: '<%= ASSETS_PATH %>/js/sonar.js'


    replace:
      lcov:
        src: 'target/web-tests/lcov.info'
        dest: 'target/web-tests/lcov.info'
        replacements: [
          { from: '/build/', to: '/src/main/' }
        ]


    rename:
      lcov:
        src: 'lcov.info'
        dest: 'target/web-tests/lcov.info'


    intern:
      test:
        options:
          runType: 'runner'
          config: 'test/intern'
          proxyPort: expressPort
          proxyUrl: 'http://localhost:' + expressPort + '/'
          useBrowserStack: useBrowserStack


    watch:
      options:
        spawn: false

      less:
        files: '<%= SOURCE_PATH %>/less/**/*.less'
        tasks: ['less:build', 'copy:assets-css']

      js:
        files: '<%= SOURCE_PATH %>/js/**/*.js'
        tasks: ['copy:js', 'babel:build', 'concat:build', 'copy:assets-all-js']

      handlebars:
        files: '<%= SOURCE_PATH %>/**/*.hbs'
        tasks: ['handlebars:build', 'copy:assets-all-js']


  # Basic tasks
  grunt.registerTask 'prepare',
      ['clean:css', 'clean:js', 'clean:build', 'less:build', 'handlebars:build', 'copy:js', 'babel:build', 'concat:build']

  grunt.registerTask 'build-fast-suffix',
      ['copy:assets-css', 'copy:assets-all-js']

  grunt.registerTask 'build-suffix',
      ['copy:assets-css', 'copy:assets-js', 'concurrent:build']

  grunt.registerTask 'test-suffix',
      ['intern:test', 'rename:lcov', 'replace:lcov']

  grunt.registerTask 'coverage-suffix',
      ['test-suffix']

  grunt.registerTask 'build-app', (app) ->
    grunt.option 'app', app
    grunt.task.run 'requirejs:app'

  grunt.registerTask 'build-widget', (widget) ->
    grunt.option 'widget', widget
    grunt.task.run 'requirejs:widget'

  # Output tasks
  grunt.registerTask 'build-fast',
      ['prepare', 'build-fast-suffix']

  grunt.registerTask 'build',
      ['prepare', 'build-suffix']

  grunt.registerTask 'build-test',
      ['prepare', 'build-suffix', 'test-suffix']

  grunt.registerTask 'build-coverage',
      ['prepare', 'build-suffix', 'coverage-suffix']

  grunt.registerTask 'test',
      ['prepare', 'test-suffix']

  grunt.registerTask 'coverage',
      ['prepare', 'coverage-suffix']

  grunt.registerTask 'default',
      ['build']

  # Development
  grunt.registerTask 'dw',
      ['build-fast', 'watch']

  # tasks used by Maven build (see pom.xml)
  grunt.registerTask 'maven-quick-build',
      ['build-fast']

  grunt.registerTask 'maven-build',
      ['build']
