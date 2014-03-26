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
        switch action
          when 'activate' then _.extend query, bulk_activate: [param]
          when 'deactivate' then _.extend query, bulk_deactivate: [param]
        @options.app.codingRulesBulkChangeView.bulkChange query


    onRender: ->
      jQuery('body').append @el
      jQuery('body').off('click.bulk-change').on 'click.bulk-change', => @hide()


    toggle: ->
      if @$el.is(':visible') then @hide() else @show()


    show: ->
      @render()
      @$el.show()


    hide: ->
      @$el.hide()


    serializeData: ->
      languages = @options.app.languageFilter.get('value')
      qualityProfile: @options.app.getQualityProfile()
      qualityProfileName: @options.app.qualityProfileFilter.view.renderValue()
      singleLanguage: _.isArray(languages) && languages.length == 1