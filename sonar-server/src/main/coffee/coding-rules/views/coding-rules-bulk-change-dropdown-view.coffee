define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesBulkChangeDropdownView extends Marionette.ItemView
    className: 'coding-rules-bulk-change-dropdown'
    template: Templates['coding-rules-bulk-change-dropdown']


    events:
      'click .coding-rules-bulk-change-dropdown-link': 'doAction'


    doAction: (e) ->
      action = jQuery(e.currentTarget).data 'action'
      param = jQuery(e.currentTarget).data 'param'
      unless param
        @options.app.codingRulesBulkChangeView.show action
      else
        query = @options.app.getQuery()
        _.extend query,
          profile_key: param
          wsAction: action
        @options.app.codingRulesBulkChangeView.bulkChange query


    onRender: ->
      jQuery('body').append @el
      jQuery('body').off('click.bulk-change').on 'click.bulk-change', => @hide()
      @$el.css
        top: jQuery('.navigator-actions').offset().top + jQuery('.navigator-actions').height() + 1
        left: jQuery('.navigator-actions').offset().left + jQuery('.navigator-actions').outerWidth() - @$el.outerWidth()


    toggle: ->
      if @$el.is(':visible') then @hide() else @show()


    show: ->
      @render()
      @$el.show()


    hide: ->
      @$el.hide()


    serializeData: ->
      languages = @options.app.languageFilter.get('value')
      activation = @options.app.activationFilter.get('value')
      qualityProfile: @options.app.getQualityProfile()
      qualityProfileName: @options.app.qualityProfileFilter.view.renderValue()
      singleLanguage: _.isArray(languages) && languages.length == 1
      language: @options.app.languageFilter.view.renderValue()
      activation: activation && activation.length == 1 && activation[0]
