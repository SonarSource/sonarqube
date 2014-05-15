define [
  'navigator/filters/ajax-select-filters'
], (
  AjaxSelectFilters
) ->

  class TagSuggestions extends AjaxSelectFilters.Suggestions

    url: ->
      "#{baseUrl}/api/rules/tags"

    parse: (r) ->
      return _.map(r.tags, (tag) ->
        new Backbone.Model
          id: tag
          text: tag
      )

  class TagFilterView extends AjaxSelectFilters.AjaxSelectFilterView

    initialize: ->
      super
      @choices = new TagSuggestions
