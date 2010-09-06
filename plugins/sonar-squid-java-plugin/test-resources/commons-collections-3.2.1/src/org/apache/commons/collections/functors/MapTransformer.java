/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections.functors;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.collections.Transformer;

/**
 * Transformer implementation that returns the value held in a specified map
 * using the input parameter as a key.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public final class MapTransformer implements Transformer, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = 862391807045468939L;
    
    /** The map of data to lookup in */
    private final Map iMap;

    /**
     * Factory to create the transformer.
     * <p>
     * If the map is null, a transformer that always returns null is returned.
     * 
     * @param map the map, not cloned
     * @return the transformer
     */
    public static Transformer getInstance(Map map) {
        if (map == null) {
            return ConstantTransformer.NULL_INSTANCE;
        }
        return new MapTransformer(map);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param map  the map to use for lookup, not cloned
     */
    private MapTransformer(Map map) {
        super();
        iMap = map;
    }

    /**
     * Transforms the input to result by looking it up in a <code>Map</code>.
     * 
     * @param input  the input object to transform
     * @return the transformed result
     */
    public Object transform(Object input) {
        return iMap.get(input);
    }

    /**
     * Gets the map to lookup in.
     * 
     * @return the map
     * @since Commons Collections 3.1
     */
    public Map getMap() {
        return iMap;
    }

}
