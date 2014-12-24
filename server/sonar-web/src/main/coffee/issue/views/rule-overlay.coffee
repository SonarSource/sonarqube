define [
  'common/overlay'
  'templates/issue'
], (
  Overlay
  Templates
) ->

  class extends Overlay
    template: Templates['issue-rule']


    serializeData: ->
      _.extend super,
        permalink: "#{baseUrl}/coding_rules/show?key=#{encodeURIComponent @model.get('key')}"
        allTags: _.union @model.get('sysTags'), @model.get('tags')
