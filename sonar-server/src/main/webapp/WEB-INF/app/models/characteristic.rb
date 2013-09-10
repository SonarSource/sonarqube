#
# Sonar, entreprise quality control tool.
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
class Characteristic < ActiveRecord::Base
  NAME_MAX_SIZE=100

  has_and_belongs_to_many :children, :class_name => 'Characteristic', :join_table => 'characteristic_edges',
    :foreign_key => 'parent_id', :association_foreign_key => 'child_id', :order => 'characteristic_order ASC'

  has_and_belongs_to_many :parents, :class_name => 'Characteristic', :join_table => 'characteristic_edges',
    :foreign_key => 'child_id', :association_foreign_key => 'parent_id'

  belongs_to :rule
  belongs_to :quality_model
  has_many :characteristic_properties, :dependent => :delete_all

  validates_uniqueness_of :name, :scope => [:quality_model_id, :enabled], :case_sensitive => false, :if => Proc.new { |c| c.rule_id.nil? && c.enabled }
  validates_length_of :name, :in => 1..NAME_MAX_SIZE, :allow_blank => false, :if => Proc.new { |c| c.rule_id.nil? }
  validates_presence_of :quality_model

  def root?
    depth==1
  end

  def key
    kee
  end

  def name(rule_name_if_empty=false)
    result=read_attribute(:name)
    if (result.nil? && rule_name_if_empty && rule_id)
      result=rule.name  
    end
    result
  end

  # return the first parent
  def parent
    parents.empty? ? nil : parents[0]
  end

  def enabled_children
    children.select{|c| c.enabled}
  end

  def properties
    characteristic_properties
  end

  def property(key)
    properties.each do |p|
      return p if p.key==key
    end
    nil
  end

  # the property is not saved
  def set_property(key, value)
    p=property(key)
    unless p
      p=characteristic_properties.build(:kee => key)
    end
    if (value.is_a?(Fixnum) || value.is_a?(Float))
      p.value=value.to_f
    else
      p.text_value=value.to_s
    end
    p
  end

  def save_property(key, value)
    p=set_property(key, value)
    p.save
  end

  def property_value(key, default_value=nil)
    p=property(key)
    if p
      (p.value ? p.value.to_f : nil) || p.text_value || default_value
    else
      default_value
    end
  end
end
