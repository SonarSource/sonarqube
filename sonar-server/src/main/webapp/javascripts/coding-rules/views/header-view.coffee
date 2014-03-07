define [
  'backbone.marionette',
  'coding-rules/views/header-quality-profiles-view',
  'common/handlebars-extensions'
], (
  Marionette,
  HeaderQualityProfilesView
) ->

  class CodingRulesHeaderView extends Marionette.ItemView
    template: getTemplate '#coding-rules-header-template'


    ui:
      menuToggle: '.navigator-header-menu-toggle'


    events:
      'click @ui.menuToggle': 'toggleQualityProfiles'


    initialize: ->
      @qualityProfilesView = new HeaderQualityProfilesView
        app: @options.app
        collection: new Backbone.Collection @options.app.qualityProfiles
        headerView: @
      jQuery('body').on 'click', =>
        @qualityProfilesView.$el.hide()
        @ui.menuToggle.removeClass 'active'


    onRender: ->
      @qualityProfilesView.render().$el.detach().appendTo jQuery('body')


    onDomRefresh: ->
      @qualityProfilesView.$el.css
        top: @$el.offset().top + @$el.outerHeight()
        left: @$el.offset().left + @ui.menuToggle.css('margin-left')


    toggleQualityProfiles: (e) ->
      e.stopPropagation()
      @qualityProfilesView.$el.toggle()
      @ui.menuToggle.toggleClass 'active'


    serializeData: ->
      _.extend super, header: @options.app.header
