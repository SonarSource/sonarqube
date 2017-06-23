<?php
/**
 * This file is part of phpUnderControl.
 *
 * Copyright (c) 2007-2009, Manuel Pichler <mapi@phpundercontrol.org>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   * Neither the name of Manuel Pichler nor the names of his
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @package   Example
 * @author    Manuel Pichler <mapi@phpundercontrol.org>
 * @copyright 2007-2009 Manuel Pichler. All rights reserved.
 * @license   http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version   SVN: $Id: Math.php 4429 2009-01-04 15:39:45Z mapi $
 * @link      http://www.phpundercontrol.org/
 */
function add($v1 , $v2)
    {
        return ($v1 + $v2);
    }

/**
 * Simple math class.
 *
 * @package   Example
 * @author    Manuel Pichler <mapi@phpundercontrol.org>
 * @copyright 2007-2009 Manuel Pichler. All rights reserved.
 * @license   http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version   Release: 0.5.0
 * @link      http://www.phpundercontrol.org/
 */
class PhpUnderControl_Example_Math
{
    /**
     * Adds the two given values.
     *
     * @param integer $v1 Value one.
     * @param integer $v2 Value two.
     *
     * @return integer.
     */
    public function add($v1 , $v2)
    {
        return ($v1 + $v2);
    }

    /**
     * Subtract param two from param one
     *
     * @param integer $v1 Value one.
     * @param integer $v2 Value two.
     *
     * @return integer.
     */
    public function sub($v1, $v2)
    {
        return ($v1 - $v2);
    }

    /**
     * Not tested method that should be visible with low coverage.
     */
    public function div($v1, $v2)
    {
        $v3 = $v1 / ($v2 + $v1);
        if ($v3 > 14)
        {
            $v4 = 0;
            for ($i = 0; $i < $v3; $i++)
            {
                $v4 += ($v2 * $i);
            }
        }
        $v5 = ($v4 < $v3 ? ($v3 - $v4) : ($v4 - $v3));

        $v6 = ($v1 * $v2 * $v3 * $v4 * $v5);

        $d = array($v1, $v2, $v3, $v4, $v5, $v6);

        $v7 = 1;
        for ($i = 0; $i < $v6; $i++)
        {
            shuffle( $d );
            $v7 = $v7 + $i * end($d);
        }

        $v8 = $v7;
        foreach ( $d as $x )
        {
            $v8 *= $x;
        }
        
        $v3 = $v1 / ($v2 + $v1);
        if ($v3 > 14)
        {
            $v4 = 0;
            for ($i = 0; $i < $v3; $i++)
            {
                $v4 += ($v2 * $i);
            }
        }
        $v5 = ($v4 < $v3 ? ($v3 - $v4) : ($v4 - $v3));

        $v6 = ($v1 * $v2 * $v3 * $v4 * $v5);

        $d = array($v1, $v2, $v3, $v4, $v5, $v6);

        $v7 = 1;
        for ($i = 0; $i < $v6; $i++)
        {
            shuffle( $d );
            $v7 = $v7 + $i * end($d);
        }

        $v8 = $v7;
        foreach ( $d as $x )
        {
            $v8 *= $x;
        }

        return $v8;
    }

    /**
     * Simple copy for cpd detection.
     */
    public function complex($v1, $v2)
    {
        $v3 = $v1 / ($v2 + $v1);
        if ($v3 > 14)
        {
            $v4 = 0;
            for ($i = 0; $i < $v3; $i++)
            {
                $v4 += ($v2 * $i);
            }
        }
        $v5 = ($v4 < $v3 ? ($v3 - $v4) : ($v4 - $v3));

        $v6 = ($v1 * $v2 * $v3 * $v4 * $v5);

        $d = array($v1, $v2, $v3, $v4, $v5, $v6);

        $v7 = 1;
        for ($i = 0; $i < $v6; $i++)
        {
            shuffle( $d );
            $v7 = $v7 + $i * end( $d );
        }

        $v8 = $v7;
        foreach ( $d as $x )
        {
            $v8 *= $x;
        }
        
        $v3 = $v1 / ($v2 + $v1);
        if ($v3 > 14)
        {
            $v4 = 0;
            for ($i = 0; $i < $v3; $i++)
            {
                $v4 += ($v2 * $i);
            }
        }
        $v5 = ($v4 < $v3 ? ($v3 - $v4) : ($v4 - $v3));

        $v6 = ($v1 * $v2 * $v3 * $v4 * $v5);

        $d = array($v1, $v2, $v3, $v4, $v5, $v6);

        $v7 = 1;
        for ($i = 0; $i < $v6; $i++)
        {
            shuffle( $d );
            $v7 = $v7 + $i * end($d);
        }

        $v8 = $v7;
        foreach ( $d as $x )
        {
            $v8 *= $x;
        }

        return $v8;
    }
}
