define [
  'backbone.marionette'
  'coding-rules/views/coding-rules-detail-quality-profile-view'
], (
  Marionette,
  CodingRulesDetailQualityProfileView
) ->

  class CodingRulesDetailQualityProfilesView extends Marionette.CollectionView
    itemView: CodingRulesDetailQualityProfileView

    itemViewOptions: ->
      app: @options.app
      qualityProfiles: @collection