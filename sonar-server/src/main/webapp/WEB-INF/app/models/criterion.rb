#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
class Criterion < ActiveRecord::Base
  set_table_name 'criteria'

  validates_inclusion_of :operator, :in => ['<','>', '=', '<=', '>='], :if => :on_metric?, :message => 'Select an operator'
  validates_presence_of :kee, :if => :on_metric?
  validates_numericality_of :value, :if => :on_metric?, :message => 'Value must be a number'

  validates_inclusion_of :operator, :in => ['='], :if => :on_qualifier?

  validates_inclusion_of :operator, :in => ['='], :if => :on_language?

  validates_inclusion_of :operator, :in => ['<','>='], :if => :on_date?
  validates_numericality_of :value, :allow_nil => false, :only_integer => true, :greater_than_or_equal_to => 1, :if => :on_date?

  validates_presence_of :text_value, :if => :on_key?
  validates_presence_of :text_value, :if => :on_name?

  belongs_to :filter

  def key
    kee
  end

  def on_metric?
    family=='metric'
  end

  def metric
    Metric.by_key(kee)
  end

  def on_name?
    family=='name'
  end

  def on_key?
    family=='key'
  end

  def on_qualifier?
    family=='qualifier'
  end

  def on_language?
    family=='language'
  end

  def on_date?
    family=='date'
  end

  def text_values
    text_value ? text_value.split(',') : []
  end

  def self.new_for_qualifiers(values)
    values=[] if values.nil?
    new(:family => 'qualifier', :operator => '=', :text_value => values.join(','))
  end

  def self.new_for_metric(options)
    metric=Metric.by_id(options['metric_id'])
    new(:family => 'metric', :operator => options['operator'], :kee => (metric ? metric.name : nil), :value => options['value'], :variation => (options['type']=='variation'))
  end

end
