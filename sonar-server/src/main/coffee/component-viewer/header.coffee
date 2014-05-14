define [
  'backbone.marionette'
  'templates/component-viewer'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class HeaderView extends Marionette.ItemView
    template: Templates['header']


    ui:
      expandLinks: '.component-viewer-header-measures-expand'
      expandedBars: '.component-viewer-header-expanded-bar'


    events:
      'click @ui.expandLinks': 'showExpandedBar'


    onRender: ->
      @delegateEvents()


    showExpandedBar: (e) ->
      el = $(e.currentTarget)
      if el.is '.active'
        @ui.expandLinks.removeClass 'active'
        @ui.expandedBars.hide()
      else
        @ui.expandLinks.removeClass 'active'
        el.addClass 'active'
        scope = el.data 'scope'
        @ui.expandedBars.hide()
        if scope
          @ui.expandedBars.filter("[data-scope=#{scope}]").show()


    serializeData: ->
      component = @options.main.component.toJSON()
      if component.measures
        component.measures.max_issues = Math.max(
          component.measures.blocker_issues
          component.measures.critical_issues
          component.measures.major_issues
          component.measures.minor_issues
          component.measures.info_issues
        )

      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: component