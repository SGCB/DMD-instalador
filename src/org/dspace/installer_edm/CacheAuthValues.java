package org.dspace.installer_edm;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: salzaru
 * Date: 11/02/13
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
public class CacheAuthValues
{
    private ArrayList<LinkedList<CacheAuthValue>> levelList;
    private HashMap<String, CacheAuthValue> hashtable;
    private int numAuth = 0;
    private int minLevel = 0;
    private static final int NUM_MAX = 100;

    public CacheAuthValues()
    {
        this(101);
    }

    public CacheAuthValues(int capacity)
    {
        hashtable = new HashMap<String, CacheAuthValue>(capacity);
        levelList = new ArrayList<LinkedList<CacheAuthValue>>();
    }

    public CacheAuthValue pollCacheAuthValue()
    {
        CacheAuthValue cacheAuthValue = null;
        if (!levelList.isEmpty() && !levelList.get(minLevel).isEmpty()) {
            cacheAuthValue = levelList.get(minLevel).removeLast();
            hashtable.remove(cacheAuthValue.getValue());
            numAuth--;
            if (levelList.get(minLevel).isEmpty()) minLevel = searchNewMinLevel(minLevel + 1);
        }
        return cacheAuthValue;
    }

    public int getNumAuth()
    {
        return numAuth;
    }

    private int searchNewMinLevel(int currentLevel)
    {
        for (int i = currentLevel; i < levelList.size(); i++)
            if (!levelList.get(i).isEmpty()) return i;
        return 0;
    }

    public void addCacheAuthValue(String value, String handle)
    {
        CacheAuthValue cacheAuthValue = searchCacheAuthValueFromValue(value);
        if (cacheAuthValue != null) {
            if (searchCacheAuthValueInLevelList(cacheAuthValue)) {
                if (numAuth == NUM_MAX) pollCacheAuthValue();
                moveCacheAuthValueDown(cacheAuthValue);
            }
        } else {
            if (numAuth == NUM_MAX) pollCacheAuthValue();
            cacheAuthValue = new CacheAuthValue(value, handle);
            if (levelList.isEmpty()) levelList.add(new LinkedList<CacheAuthValue>());
            levelList.get(0).addFirst(cacheAuthValue);
            hashtable.put(value, cacheAuthValue);
            minLevel = 0;
            numAuth++;
        }
    }


    private CacheAuthValue moveCacheAuthValueDown(CacheAuthValue cacheAuthValue)
    {
        ListIterator<LinkedList<CacheAuthValue>> iterator = levelList.listIterator();
        LinkedList<CacheAuthValue> cacheAuthValueList = searchLevel(cacheAuthValue.getNumMatches(), iterator);
        if (cacheAuthValueList != null && cacheAuthValueList.remove(cacheAuthValue)) {
            int currentLevel = cacheAuthValue.getNumMatches();
            cacheAuthValue.incNumMatches();
            LinkedList<CacheAuthValue> cacheAuthValueNewList = null;
            if (!iterator.hasNext()) {
                cacheAuthValueNewList = new LinkedList<CacheAuthValue>();
                levelList.add(cacheAuthValueNewList);
            } else {
                cacheAuthValueNewList = iterator.next();
            }
            cacheAuthValueNewList.addFirst(cacheAuthValue);
            if (currentLevel == minLevel && cacheAuthValueList.isEmpty()) minLevel = searchNewMinLevel(currentLevel);
        }
        return null;
    }


    public boolean searchCacheAuthValueInLevelList(CacheAuthValue cacheAuthValue)
    {
        LinkedList<CacheAuthValue> cacheAuthValueList = searchLevel(cacheAuthValue.getValue(), cacheAuthValue.getNumMatches());
        if (cacheAuthValueList != null) return searchCacheAuthValueInList(cacheAuthValueList, cacheAuthValue);
        return false;
    }

    private LinkedList<CacheAuthValue> searchLevel(String value, int level)
    {
        return searchLevel(level, levelList.listIterator());
    }

    private LinkedList<CacheAuthValue> searchLevel(int level, ListIterator<LinkedList<CacheAuthValue>> iterator)
    {
        while (iterator.hasNext()) {
            LinkedList<CacheAuthValue> listAux = iterator.next();
            if (!listAux.isEmpty() && listAux.getFirst().getNumMatches() == level) {
                return listAux;
            }
        }
        return null;
    }

    private boolean searchCacheAuthValueInList(LinkedList<CacheAuthValue> list, CacheAuthValue cacheAuthValue)
    {
        for (CacheAuthValue cacheAuthValueAux : list) {
            if (cacheAuthValueAux.equals(cacheAuthValue)) return true;
        }
        return false;
    }


    public CacheAuthValue searchCacheAuthValueFromValue(String value)
    {
        if (!hashtable.isEmpty() && hashtable.containsKey(value)) {
            return hashtable.get(value);
        }
        return null;
    }

    public String searchHandleFromValue(String value)
    {
        if (!hashtable.isEmpty() && hashtable.containsKey(value)) {
            return hashtable.get(value).getHandle();
        }
        return null;
    }

}


class CacheAuthValue
{
    private String handle;
    private String value;
    private int numMatches;


    public CacheAuthValue(String value, String handle)
    {
        this.numMatches = 1;
        this.handle = handle;
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public String getHandle()
    {
        return handle;
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
