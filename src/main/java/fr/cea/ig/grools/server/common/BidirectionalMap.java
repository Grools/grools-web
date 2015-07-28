package fr.cea.ig.grools.server.common;
/*
 * Copyright LABGeM 24/02/15
 *
 * author: Jonathan MERCIER
 *
 * This software is a computer program whose purpose is to annotate a complete genome.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */


import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
/*
 * @startuml
 * class BidirectionalMap implements BiMap{
 * }
 * @enduml
 */
public class BidirectionalMap<K,V> implements BiMap{
    private final Map<K,V> map1;
    private final Map<V,K> map2;

    public BidirectionalMap(){
        map1 = new ConcurrentHashMap<K,V>();
        map2 = new ConcurrentHashMap<V,K>();
    }

    public BidirectionalMap( final Map<K,V> m1, final Map<V,K> m2 ){
        map1 = new ConcurrentHashMap<K,V>(); map1.putAll(m1);
        map2 = new ConcurrentHashMap<V,K>(); map2.putAll(m2);
    }

    @Override
    public V put(final Object key, final Object value){
        V v = map1.put( (K) key,   (V) value);
        map2.put( (V) value, (K)key);
        return v;
    }

    @Override
    public V get(final Object key){
        return map1.get( (K) key);
    }


    @Override
    public int size() {
        return map1.size();
    }

    @Override
    public boolean isEmpty() {
        return map1.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return map1.containsKey(key);
    }


    @Override
    public boolean containsValue(final Object value) {
        return map1.containsValue((K)value);
    }


    @Override
    public V remove(final Object key) {
        return map1.remove((K) key) ;
    }

    @Override
    public void putAll(final Map m) {
        map1.putAll(m);
        for( final Map.Entry entry : ((Map<K,V>)m).entrySet()){
            map2.put( (V) entry.getValue(), (K) entry.getKey() );
        }
    }

    @Override
    public void clear() {
        map1.clear();
        map2.clear();
    }


    @Override
    public Set<V> values(){
        return map2.keySet();
    }


    @Override
    public Set<K> keySet(){
        return map1.keySet();
    }


    @Override
    public Set<Map.Entry<K,V>> entrySet(){
        return map1.entrySet();
    }

    public BidirectionalMap<V,K> inverse(){
        return new BidirectionalMap(map2,map1);
    }


}
