/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.guice.server;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

class TransactionBasedScoper implements Scope {

    private static final int MAX_SIZE = 512;
    private final ThreadLocal<Map<Key, Object>> caches = new ThreadLocal<Map<Key, Object>>();

    public void startTransaction() {
        Map<Key, Object> cache = caches.get();

        if (cache == null) {
            caches.set(new HashMap<Key, Object>());
        } else {
            checkState(cache.isEmpty());
        }
    }

    public void endTransaction() {
        Map<Key, Object> cache = caches.get();

        if(cache.size() > MAX_SIZE){
            caches.remove();
        } else {
            cache.clear();
        }
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T get() {
                Map<Key, Object> cache = caches.get();

                T t = (T) cache.get(key);

                if (t == null) {
                    t = unscoped.get();
                    cache.put(key, t);
                }

                return t;
            }
        };
    }
}
