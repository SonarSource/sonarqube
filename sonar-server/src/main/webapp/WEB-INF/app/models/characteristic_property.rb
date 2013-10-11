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
class CharacteristicProperty < ActiveRecord::Base
  KEY_MAX_SIZE=100

  PROPERTY_REMEDIATION_FUNCTION = "remediationFunction";
  PROPERTY_REMEDIATION_FACTOR = "remediationFactor";
  PROPERTY_OFFSET = "offset";

  FUNCTION_CONSTANT = "constant_resource";
  FUNCTION_LINEAR = "linear";
  FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";
  FUNCTION_LINEAR_WITH_THRESHOLD = "linear_threshold";

  DAY = "d"
  HOUR = "h"
  MINUTE = "mn"

  belongs_to :characteristic
  validates_length_of :kee, :in => 1..KEY_MAX_SIZE, :allow_blank => false
  
  def key
    kee
  end

  def constant?
    text_value == FUNCTION_CONSTANT
  end

  def linear?
    text_value == FUNCTION_LINEAR
  end

  def linearWithThreshold?
    text_value == FUNCTION_LINEAR_WITH_THRESHOLD
  end

  def linearWithOffset?
    text_value == FUNCTION_LINEAR_WITH_OFFSET
  end

end
