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
package org.apache.commons.collections;

/**
 * Marker interface for collections, maps and iterators that are unmodifiable.
 * <p>
 * This interface enables testing such as:
 * <pre>
 * if (coll instanceof Unmodifiable) {
 *   coll = new ArrayList(coll);
 * }
 * // now we know coll is modifiable
 * </pre>
 * Of course all this only works if you use the Unmodifiable classes defined
 * in this library. If you use the JDK unmodifiable class via java util Collections
 * then the interface won't be there.
 *  
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Stephen Colebourne
 */
public interface Unmodifiable {
    // marker interface - no methods to implement
}
