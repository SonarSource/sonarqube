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
  STATE_ACTIVE='A'
  STATE_INACTIVE='I'
  
  has_many :widget_properties, :dependent => :delete_all
  belongs_to :dashboards

  validates_presence_of     :name
  validates_length_of       :name,    :within => 1..256

  validates_presence_of     :widget_key
  validates_length_of       :widget_key, :within => 1..256

  validates_length_of       :description,  :maximum => 1000, :allow_blank => true, :allow_nil => true

  def state
    read_attribute(:state) || 'V'
  end

  #---------------------------------------------------------------------
  # WIDGET PROPERTIES
  #---------------------------------------------------------------------
  def properties
    widget_properties
  end
  
  def widget_property(key)
    widget_properties().each do |p|
      return p if (p.key==key)
    end
    nil
  end

  def widget_property_value(key)
    prop=widget_property(key)
    prop ? prop.value : nil
  end

  def set_widget_property(options)
    key=options[:kee]
    prop=widget_property(key)
    if prop
      prop.attributes=options
      prop.widget_id=id
      prop.save!
    else
      prop=WidgetProperty.new(options)
      prop.widget_id=id
      widget_properties<<prop
    end
  end

  def delete_widget_property(key)
    prop=widget_property(key)
    if prop
      widget_properties.delete(prop)
    end
  end

  def properties_as_hash
    hash={}
    widget_properties.each do |prop|
      hash[prop.key]=prop.value
    end
    hash
  end
end