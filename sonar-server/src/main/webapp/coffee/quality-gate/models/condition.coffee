define [
  'backbone'
], (
  Backbone
) ->

  class Condition extends Backbone.Model

    url: ->
      "#{baseUrl}/api/qualitygates/create_condition"


    save: ->
      method = unless @isNew() then 'update' else 'create'
      data =
        metric: @get('metric').key
        op: @get('op')
        warning: @get('warning')
        error: @get('error')

      unless @get('period') == '0'
        data.period = @get('period')

      unless @isNew()
        data.id = @id
      else
        data.gateId = @get('gateId')

      jQuery.ajax({
        url: "#{baseUrl}/api/qualitygates/#{method}_condition"
        type: 'POST'
        data: data
      }).done (r) =>
        @set 'id', r.id


    delete: ->
      jQuery.ajax
        url: "#{baseUrl}/api/qualitygates/delete_condition"
        type: 'POST'
        data: id: @id

