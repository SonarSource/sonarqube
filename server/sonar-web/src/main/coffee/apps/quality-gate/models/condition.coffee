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

define ->

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

