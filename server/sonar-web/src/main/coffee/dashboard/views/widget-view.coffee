#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define [
  'templates/dashboard'
], (
  Templates
) ->

  $ = jQuery


  class extends Marionette.ItemView
    template: Templates['widget']
    className: 'block'


    events:
      'click .js-edit-widget': 'editWidget'
      'click .js-cancel-edit-widget': 'cancelEditWidget'
      'submit .js-edit-widget-form': 'saveWidget'


    initialize: (options) ->
#      @listenTo options.app.state, 'change', @render
      if @model.get 'configured'
        @requestContent()


    onRender: ->
      @$el.data 'id', @model.id
      if @options.app.state.get 'configure'
        @$el.prop 'draggable', true


    requestContent: ->
      payload = id: @model.get 'key'
      if @options.app.resource
        payload.resource = @options.app.resource
      if @model.has 'componentId'
        payload.resource = @model.get 'componentId'
      _.extend payload, @getWidgetProps()
      $.get "#{baseUrl}/widget/show", payload, (html) =>
        @model.set 'html', html
        @render()


    getWidgetProps: ->
      properties = @model.get 'props'
      r = {}
      properties.forEach (prop) ->
        r[prop.key] = prop.val if prop.val?
      r


    editWidget: ->
      $.get "#{baseUrl}/api/dashboards/configure_widget", id: @model.get('key'), (data) =>
        @model.mergeProperties data.widget.properties
        @showEditForm()


    showEditForm: (render = true) ->
      @render() if render
      @$('.widget_props').removeClass 'hidden'
      @$('.configure_widget').addClass 'hidden'


    cancelEditWidget: ->
      @$('.widget_props').addClass 'hidden'
      @$('.configure_widget').removeClass 'hidden'


    saveWidget: (e) ->
      e.preventDefault()
      data = id: @model.id
      @$('.js-edit-widget-form').serializeArray().forEach (p) ->
        data[p.name] = p.value
      $.post "#{baseUrl}/api/dashboards/save_widget", data, (data) =>
        @model.set data.widget
        @requestContent()


    serializeData: ->
      _.extend super,
        baseUrl: baseUrl
        state: @options.app.state.toJSON()
