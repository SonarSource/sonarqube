define [
  'common/modals'
  'templates/issue'
], (
  ModalView
  Templates
) ->

  class extends ModalView
    template: Templates['issue-rule']


    serializeData: ->
      _.extend super,
        permalink: "#{baseUrl}/coding_rules/show?key=#{encodeURIComponent @model.get('key')}"
        allTags: _.union @model.get('sysTags'), @model.get('tags')
