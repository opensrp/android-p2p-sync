package android.util;

import java.util.HashMap;


/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 24-07-2020.
 */

public class SparseArray<E> {

    private HashMap<Integer, E> mHashMap;

    public SparseArray() {
        mHashMap = new HashMap<>();
    }

    public void put(int key, E value) {
        mHashMap.put(key, value);
    }

    public E get(int key) {
        return mHashMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public E get(int key, E valueIfKeyNotFound) {
        return mHashMap.getOrDefault(key, valueIfKeyNotFound);
    }

    public void delete(int key) {
        mHashMap.remove(key);
    }

    public E removeReturnOld(int key) {
        return mHashMap.remove(key);
    }

    /**
     * Alias for {@link #delete(int)}.
     */
    public void remove(int key) {
        delete(key);
    }

    public void removeAt(int index) {
        // Do nothing for now
    }

    public void removeAtRange(int index, int size) {
        // Do nothing for now
    }

    public int size() {
        return mHashMap.size();
    }

    public int keyAt(int index) {
        return -1;
    }

    @SuppressWarnings("unchecked")
    public E valueAt(int index) {
        return mHashMap.get(index);
    }

    public void setValueAt(int index, E value) {
        mHashMap.put(index, value);
    }

    public int indexOfKey(int key) {
        return key;
    }

    public int indexOfValue(E value) {
        return -1;
    }

    public int indexOfValueByValue(E value) {
        return -1;
    }

    public void clear() {
        mHashMap.clear();
    }

    public void append(int key, E value) {
        put(key, value);
    }

    @Override
    public String toString() {
        if (size() <= 0) {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(mHashMap.size() * 28);
        buffer.append('{');
        for (int i = 0; i< mHashMap.size(); i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            int key = keyAt(i);
            buffer.append(key);
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }
}