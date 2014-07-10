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

#
# Sonar 4.0
# SONAR-4416 - Migrate saved timeline chart heights to adapt to new render code (chartHeight += 100)
#

class ResizeTimelineWidgets < ActiveRecord::Migration

  class Widget < ActiveRecord::Base
  end

  class WidgetProperty < ActiveRecord::Base
  end

  def self.up
    Widget.reset_column_information
    WidgetProperty.reset_column_information

    Widget.find(:all, :conditions => { :widget_key => 'timeline' }).each do |widget|
      WidgetProperty.find(:all, :conditions => { :widget_id => widget.id, :kee => 'chartHeight' }).each do |property|
        property.update_attributes!(:text_value  => (property.read_attribute(:text_value).to_i + 100).to_s)
      end
    end
  end
end
