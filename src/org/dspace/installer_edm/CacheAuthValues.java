package org.dspace.installer_edm;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 11/02/13
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
public class CacheAuthValues
{
    private PriorityQueue<CacheAuthValue> priorityQueue;
    private HashMap<String, String> hashtable;

    public CacheAuthValues(int capacity)
    {
        hashtable = new HashMap<String, String>(capacity);
        priorityQueue = new PriorityQueue<CacheAuthValue>(capacity, new ComparatorCacheAuthValue());
    }

    public CacheAuthValue pollCacheAuthValue()
    {
        CacheAuthValue cacheAuthValue = priorityQueue.poll();
        hashtable.remove(cacheAuthValue.getValue());
        return cacheAuthValue;
    }

    public void addCacheAuthValue(String value, String handle)
    {
        CacheAuthValue cacheAuthValue = new CacheAuthValue(value);
        priorityQueue.add(cacheAuthValue);
        hashtable.put(value, handle);
    }

    public String searchHandleFromValue(String value)
    {
        if (!hashtable.isEmpty()) {

        }
        return null;
    }

}


class ComparatorCacheAuthValue implements Comparator<CacheAuthValue>
{

    @Override
    public int compare(CacheAuthValue x, CacheAuthValue y)
    {
        return Integer.valueOf(x.getNumMatches()).compareTo(Integer.valueOf(y.getNumMatches()));
    }
}

class CacheAuthValue
{
    private String value;
    private int numMatches;


    public CacheAuthValue(String value)
    {
        this.numMatches = 1;
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public int getNumMatches()
    {
        return numMatches;
    }

    public void incNumMatches()
    {
        numMatches++;
    }
}
