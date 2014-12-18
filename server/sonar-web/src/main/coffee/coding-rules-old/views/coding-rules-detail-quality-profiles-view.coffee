define [
  'backbone.marionette'
  'coding-rules-old/views/coding-rules-detail-quality-profile-view'
], (
  Marionette,
  CodingRulesDetailQualityProfileView
) ->

  class CodingRulesDetailQualityProfilesView extends Marionette.CollectionView
    itemView: CodingRulesDetailQualityProfileView

    itemViewOptions: ->
      app: @options.app
      rule: @options.rule
      qualityProfiles: @collection
