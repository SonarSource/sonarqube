define [
  'coding-rules/views/filters/profile-dependent-filter-view'
], (
  ProfileDependentFilterView
) ->

  class InheritanceFilterView extends ProfileDependentFilterView

    onChangeQualityProfile: ->
      qualityProfileKey = @qualityProfileFilter.get 'value'
      if _.isArray(qualityProfileKey) && qualityProfileKey.length == 1
        qualityProfile = @options.app.getQualityProfileByKey qualityProfileKey[0]
        if qualityProfile.parentKey
          parentQualityProfile = @options.app.getQualityProfile qualityProfile.parentKey
          if parentQualityProfile
            @makeActive()
          else
            @makeInactive()
        else
          @makeInactive()
      else
        @makeInactive()
