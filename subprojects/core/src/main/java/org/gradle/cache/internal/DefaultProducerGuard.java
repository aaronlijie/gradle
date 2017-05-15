/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.cache.internal;

import com.google.common.util.concurrent.Striped;
import org.gradle.internal.Factory;

import java.util.concurrent.locks.Lock;

public class DefaultProducerGuard<T> implements ProducerGuard<T> {
    private final Striped<Lock> locks = Striped.lock(8);

    /**
     * Synchronizes access to some resource, by making sure that 2 threads do not try to produce it at the same time.
     * The resource to be accessed is represented by the key, and the factory is whatever needs to be done to produce it.
     * This is <b>not</b> a cache: it only guards access to the resource, making sure that for the same key, there cannot
     * be 2 producers running concurrently. But once the first one finished producing something, the second factory will
     * be called. In other words, the factory should take care of caching whenever it makes sense.
     *
     * @param key the key used to synchronize access to a resource
     * @param factory the code that will produce a value for the given key
     * @param <V> the type of the value returned by the producer
     * @return the value produced by the factory
     */
    @Override
    public <V> V guardByKey(T key, Factory<V> factory) {
        Lock lock = locks.get(key);
        try {
            lock.lock();
            return factory.create();
        } finally {
            lock.unlock();
        }
    }
}
