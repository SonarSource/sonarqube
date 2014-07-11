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
class Api::WidgetsController < Api::ApiController

  def index
    widget_definitions = java_facade.getWidgets().to_a.sort_by{|w| w.getId()}
    render :json => jsonp(widgets_to_json(widget_definitions))
  end

  private

  def widgets_to_json(widget_definitions)
    json=[]
    widget_definitions.each do |widget_definition|
      hash={
        :id => widget_definition.getId(),
        :title => Api::Utils.message("widget.#{widget_definition.getId()}.name", :default => widget_definition.getTitle()),
        :description => Api::Utils.message("widget.#{widget_definition.getId()}.description", :default => widget_definition.getDescription())
      }
      if widget_definition.getWidgetCategories()
        hash[:categories]=widget_definition.getWidgetCategories()
      end
      json<<hash
    end
    json
  end
end
