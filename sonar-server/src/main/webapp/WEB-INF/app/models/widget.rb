#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class Widget < ActiveRecord::Base
  has_many :properties, :dependent => :delete_all, :class_name => 'WidgetProperty'
  belongs_to :dashboards

  validates_presence_of     :name
  validates_length_of       :name,    :within => 1..256

  validates_presence_of     :widget_key
  validates_length_of       :widget_key, :within => 1..256

  #---------------------------------------------------------------------
  # WIDGET PROPERTIES
  #---------------------------------------------------------------------
  def property(key)
    properties().each do |p|
      return p if (p.key==key)
    end
    nil
  end

  def property_value(key, default_value=nil)
    prop=property(key)
    (prop ? prop.value : nil) || default_value
  end

  def set_property(key, value, value_type)
    prop=property(key)
    if prop
      prop.text_value=value
      prop.value_type=value_type
    else
      prop=self.properties.build(:kee => key, :text_value => value, :value_type => value_type)
    end
    properties_as_hash[key]=prop.typed_value
  end

  def unset_property(key)
    prop=property(key)
    self.properties.delete(prop) if prop
  end

  def delete_property(key)
    prop=property(key)
    if prop
      properties.delete(prop)
    end
  end

  def properties_as_hash
    @properties_hash ||=
      begin
        hash={}
        properties.each do |prop|
          hash[prop.key]=prop.value
        end
        hash
      end
    @properties_hash
  end
end