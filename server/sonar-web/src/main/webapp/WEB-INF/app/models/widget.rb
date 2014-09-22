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
class Widget < ActiveRecord::Base
  has_many :properties, :dependent => :delete_all, :class_name => 'WidgetProperty', :inverse_of => :widget
  belongs_to :dashboards
  belongs_to :resource, :class_name => 'Project'

  validates_presence_of :widget_key
  validates_length_of :widget_key, :within => 1..256

  def property(property_key)
    self.properties().each do |p|
      return p if (p.key==property_key)
    end
    nil
  end

  def key
    widget_key
  end

  def html_id
    "block_#{id}"
  end

  # The parameter 'default_value' must NOT be used ! It's defined only for backward-compatibility with the SQALE plugin
  def property_value(property_key, default_value=nil)
    prop=property(property_key)
    if default_value && property_key=='depth'
      # we're in SQALE.... Keep the text_value
      result = (prop && prop.text_value)||default_value
    else
      result = (prop && prop.value)
      unless result
        property_definition=java_definition.getWidgetProperty(property_key)
        result = WidgetProperty.text_to_value(property_definition.defaultValue(), property_definition.type().name()) if property_definition
      end
    end
    result
  end

  def property_text_value(key)
      prop=property(key)
      prop && prop.text_value
    end

  def properties_as_hash
    @properties_hash ||=
      begin
        hash={}
        java_definition.getWidgetProperties().each do |property_definition|
          prop = property(property_definition.key)
          if prop
            hash[property_definition.key]=prop.value
          elsif !property_definition.defaultValue().blank?
            hash[property_definition.key]=WidgetProperty.text_to_value(property_definition.defaultValue(), property_definition.type().name())
          end
        end
        hash
      end
  end

  def java_definition
    Api::Utils.java_facade.getWidget(key)
  end

  def layout
    java_definition.getWidgetLayout().name()
  end
end
