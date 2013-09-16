package com.tinkerpop.blueprints.tinkergraph;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.IndexHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerIndex<T extends Element> implements Serializable {

    protected Map<String, Map<Object, Set<T>>> index = new HashMap<>();
    protected final Class<T> indexClass;
    private final Set<String> indexedKeys = new HashSet<>();
    private final TinkerGraph graph;

    public TinkerIndex(final TinkerGraph graph, final Class<T> indexClass) {
        this.graph = graph;
        this.indexClass = indexClass;
    }

    public void put(final String key, final Object value, final T element) {
        Map<Object, Set<T>> keyMap = this.index.get(key);
        if (keyMap == null) {
            keyMap = new HashMap<>();
            this.index.put(key, keyMap);
        }
        Set<T> objects = keyMap.get(value);
        if (null == objects) {
            objects = new HashSet<>();
            keyMap.put(value, objects);
        }
        objects.add(element);

    }

    public List<T> get(final String key, final Object value) {
        final Map<Object, Set<T>> keyMap = this.index.get(key);
        if (null == keyMap) {
            return Collections.emptyList();
        } else {
            Set<T> set = keyMap.get(value);
            if (null == set)
                return Collections.emptyList();
            else
                return new ArrayList<>(set);
        }
    }

    public long count(final String key, final Object value) {
        final Map<Object, Set<T>> keyMap = this.index.get(key);
        if (null == keyMap) {
            return 0;
        } else {
            Set<T> set = keyMap.get(value);
            if (null == set)
                return 0;
            else
                return set.size();
        }
    }

    public void remove(final String key, final Object value, final T element) {
        final Map<Object, Set<T>> keyMap = this.index.get(key);
        if (null != keyMap) {
            Set<T> objects = keyMap.get(value);
            if (null != objects) {
                objects.remove(element);
                if (objects.size() == 0) {
                    keyMap.remove(value);
                }
            }
        }
    }

    public void removeElement(final T element) {
        if (this.indexClass.isAssignableFrom(element.getClass())) {
            for (Map<Object, Set<T>> map : index.values()) {
                for (Set<T> set : map.values()) {
                    set.remove(element);
                }
            }
        }
    }


    public void autoUpdate(final String key, final Object newValue, final Object oldValue, final T element) {
        if (this.indexedKeys.contains(key)) {
            if (oldValue != null)
                this.remove(key, oldValue, element);
            this.put(key, newValue, element);
        }
    }

    public void autoRemove(final String key, final Object oldValue, final T element) {
        if (this.indexedKeys.contains(key)) {
            this.remove(key, oldValue, element);
        }
    }

    public void createKeyIndex(final String key) {
        if (this.indexedKeys.contains(key))
            return;

        this.indexedKeys.add(key);

        if (TinkerVertex.class.equals(this.indexClass)) {
            IndexHelper.reIndexElements(graph, graph.query().vertices(), new HashSet<>(Arrays.asList(key)));
        } else {
            IndexHelper.reIndexElements(graph, graph.query().edges(), new HashSet<>(Arrays.asList(key)));
        }
    }

    public void dropKeyIndex(final String key) {
        this.index.remove(key).clear();
    }

    public Set<String> getIndexedKeys() {
        return this.index.keySet();
    }
}
