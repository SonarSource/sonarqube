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
  'quality-gate/models/quality-gate'
], (
  QualityGate
) ->

  class QualityGates extends Backbone.Collection
    model: QualityGate


    url: ->
      "#{baseUrl}/api/qualitygates/list"


    # {
    #   "qualitygates": [
    #     { "id": 42, "name": "QG 1" },
    #     { "id": 43, "name": "QG 2" },
    #     { "id": 44, "name": "QG 3" }
    #   ],
    #   "default": 42
    # }
    parse: (r) ->
      r.qualitygates.map (gate) ->
        _.extend gate, default: gate.id == r.default


    comparator: (item) -> item.get('name').toLowerCase()
