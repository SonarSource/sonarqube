#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2013 SonarSource
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
# Sonar 3.4
#
class MoveFilterWidgets < ActiveRecord::Migration

  class MeasureFilter < ActiveRecord::Base
  end

  class OldFilter < ActiveRecord::Base
    set_table_name :filters
  end

  class Widget < ActiveRecord::Base
  end

  class WidgetProperty < ActiveRecord::Base
  end

  class Dashboard < ActiveRecord::Base
  end

  def self.up
    widgets = Widget.find(:all, :conditions => ["widget_key='filter'"])
    say_with_time "Update #{widgets.size} widgets" do
      widgets.each do |widget|
        dashboard = Dashboard.find_by_id(widget.dashboard_id)
        widget_property = WidgetProperty.find(:first, :conditions => {:widget_id => widget.id, :kee => 'filter'})
        if dashboard && widget_property && widget_property.text_value
          old_filter = OldFilter.find_by_kee(widget_property.text_value)
          if old_filter
            filter = MeasureFilter.find(:first, :conditions => ['name=? and user_id=?', old_filter.name, old_filter.user_id]) if old_filter.user_id
            filter = MeasureFilter.find(:first, :conditions => ['name=? and user_id is null', old_filter.name]) unless filter
            if filter
              widget_property.text_value=filter.id.to_s
              widget_property.save
              widget.widget_key=(filter.data.include?('display=treemap') ? 'measure_filter_treemap' : 'measure_filter_list')
              widget.save
            end
          end
        end
      end
    end
  end
end
