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
 */

require_once dirname(__FILE__) . '/../../src/php/Math.php';

/**
 * Simple math test class.
 *
 * @package   Example
 * @author    Manuel Pichler <mapi@phpundercontrol.org>
 * @copyright 2007-2009 Manuel Pichler. All rights reserved.
 * @license   http://www.opensource.org/licenses/bsd-license.php  BSD License
 * @version   Release: 0.5.0
 * @link      http://www.phpundercontrol.org/
 */
class PhpUnderControl_Example_MathTest extends PHPUnit_Framework_TestCase
{
    /**
     * The used math object.
     *
     * @var PhpUnderControl_Example_Math $math
     */
    protected $math = null;

    /**
     * Creates a new {@link PhpUnderControl_Example_Math} object.
     */
    public function setUp()
    {
        parent::setUp();

        $this->math = new PhpUnderControl_Example_Math();
    }

    /**
     * Successful test.
     */
    public function testAddSuccess()
    {
        sleep(2);
        $this->assertEquals(4, $this->math->add(1, 3));
    }

    /**
     * Successful test.
     */
    public function testSubSuccess()
    {
        $this->assertEquals( -2, $this->math->sub( 1, 3 ) );
    }
    
    /**
     * Failing test.
     */
    public function testSubFail()
    {
        sleep(2);
        $this->assertEquals( 0, $this->math->sub( 2, 1 ) );
    }
    
    /**
     * Test case with data provider.
     *
     * @dataProvider dataProviderOne
     */
    public function testDataProviderOneWillFail( $x, $y )
    {
         sleep(1);
        $this->assertEquals( 1, $this->math->sub( $x, $y ) );
    }
    
    /**
     * Test case with data provider.
     *
     * @dataProvider dataProviderTwo
     */
    public function testDataProviderAllWillFail( $x, $y )
    {
        $this->assertEquals( 1, $this->math->sub( $x, $y ) );
    }
    
    /**
     * Failing test.
     */
    public function testFail()
    {
        $this->fail('Failed because...');
    }
    
    /**
     * Skipping test.
     */
    public function testMarkSkip()
    {
        $this->markTestSkipped('Skipped because...');
    }
    
    /**
     * Skipping test.
     */
    public function testMarkIncomplete()
    {
        $this->markTestIncomplete('Incomplete because...');
    }
    
    /**
     * Example data provider.
     *
     * @return array(array)
     */
    public static function dataProviderOne()
    {
        return array(
            array( 2, 1 ),
            array( 3, 2 ),
            array( 7, 1 ),
            array( 9, 8 ),
        );
    }
    
    /**
     * Example data provider.
     *
     * @return array(array)
     */
    public static function dataProviderTwo()
    {
        return array(
            array( 17, 42 ),
            array( 13, 23 ),
            array( 42, 17 ),
            array( 23, 13 ),
        );
    }
}
