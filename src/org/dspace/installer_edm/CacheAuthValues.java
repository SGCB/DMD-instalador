/**
 *  Copyright 2013 Spanish Minister of Education, Culture and Sport
 *
 *  written by MasMedios
 *
 *  Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work  except in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://ec.europa.eu/idabc/servlets/Docbb6d.pdf?id=31979
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis,
 *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.dspace.installer_edm;

import java.util.*;

/**
 * @class CacheAuthValues
 *
 * Clase para cachear lso valores de las autoridades mediante tablas hash y listas
 * para consultar menos a la base de datos cuando se buscan si existen valores de autoridades
 *
 */
public class CacheAuthValues
{
    /**
     * Array con la lista de los niveles de autoridades. Cada nivel indica las concidencias en la autoridad.
     * Se desechan cuando se llena la caché los de menos nivel.
     */
    private ArrayList<LinkedList<CacheAuthValue>> levelList;

    /**
     * Tabla hash con clave el valor de la autoridad y el POJO con sus valores
     */
    private HashMap<String, CacheAuthValue> hashtable;

    /**
     * número de autoridades cacheadas actualmente
     */
    private int numAuth = 0;

    /**
     * nivel mínimo o mínimo número de coincidencias actual
     */
    private int minLevel = 0;

    /**
     * número máximo de autoridades a cachear
     */
    private static final int NUM_MAX = 100;

    /**
     * Constructor, se incializa con 101 como capacidad para la tabla hash
     */
    public CacheAuthValues()
    {
        this(101);
    }

    /**
     * Constructor, crea la tabla hash el array de listas
     *
     * @param capacity capacidad para la tabla hash
     */
    public CacheAuthValues(int capacity)
    {
        hashtable = new HashMap<String, CacheAuthValue>(capacity);
        levelList = new ArrayList<LinkedList<CacheAuthValue>>();
    }

    /**
     * Devuelve y elimina el POJO de la autoridad con nivel mínimo.
     * Busca el nivel mínimo actual tras la operación
     *
     * @return POJO de la autoridad
     */
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

    /**
     * Devuelve el número de autoridades en la caché
     *
     * @return número de autoridades en la caché
     */
    public int getNumAuth()
    {
        return numAuth;
    }

    /**
     * Busca el primer nivel no vacío con autoridades
     *
     * @param currentLevel nivel actual
     * @return primer nivel no vacío con autoridades
     */
    private int searchNewMinLevel(int currentLevel)
    {
        for (int i = currentLevel; i < levelList.size(); i++)
            if (!levelList.get(i).isEmpty()) return i;
        return 0;
    }

    /**
     * Añade una nueva autoridad a la caché. Primero busca si ya existe en la tabla hash.
     * Si existe lo mueve un nivel más alto,
     * si no, lo añade a la tabla hash y al nivel 0 del array.
     *
     * @param value valor de la autoridad
     * @param handle handle de la autoridad
     */
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

    /**
     * Se mueve el POJO de la autoridad un nivel más alto en el array
     * Se busca su nivel actual y se elimina de él. Si no existe nivel para el nuevo, se crea.
     * Por último se añade el POJO al nuevo nivel.
     *
     * @param cacheAuthValue POJO de la autoridad {@link CacheAuthValue}
     * @return null
     */
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

    /**
     * Busca un POJO en su nivel
     *
     * @param cacheAuthValue POJO de la autoridad {@link CacheAuthValue}
     * @return si existe en ese nivel
     */
    public boolean searchCacheAuthValueInLevelList(CacheAuthValue cacheAuthValue)
    {
        LinkedList<CacheAuthValue> cacheAuthValueList = searchLevel(cacheAuthValue.getNumMatches());
        if (cacheAuthValueList != null) return searchCacheAuthValueInList(cacheAuthValueList, cacheAuthValue);
        return false;
    }

    /**
     * Busca la lista de POJOs para el nivel suministrado
     *
     * @param level nivel a buscar
     * @return la lista con ese nivel o null
     */
    private LinkedList<CacheAuthValue> searchLevel(int level)
    {
        return searchLevel(level, levelList.listIterator());
    }

    /**
     * Busca la lista de POJOs para el nivel suministrado
     *
     *
     * @param level nivel a buscar
     * @param iterator puntero para recorrer los niveles
     * @return la lista con ese nivel o null
     */
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

    /**
     * Buscar un POJO de autoridad en la lista de su nivel
     *
     * @param list lista con su nivel de POJOs
     * @param cacheAuthValue POJO con la autoridad {@link CacheAuthValue}
     * @return si existe en ese nivel
     */
    private boolean searchCacheAuthValueInList(LinkedList<CacheAuthValue> list, CacheAuthValue cacheAuthValue)
    {
        for (CacheAuthValue cacheAuthValueAux : list) {
            if (cacheAuthValueAux.equals(cacheAuthValue)) return true;
        }
        return false;
    }

    /**
     * Buscar un POJO de autoridad en la tabla hash mediante su valor
     *
     * @param value valor de la autoridad
     * @return POJO de autoridad {@link CacheAuthValue} o null
     */
    public CacheAuthValue searchCacheAuthValueFromValue(String value)
    {
        if (!hashtable.isEmpty() && hashtable.containsKey(value)) {
            return hashtable.get(value);
        }
        return null;
    }

    /**
     * Buscar el handle de un POJO de autoridad en la tabla hash mediante su valor
     *
     * @param value valor de la autoridad
     * @return el handle o null
     */
    public String searchHandleFromValue(String value)
    {
        if (!hashtable.isEmpty() && hashtable.containsKey(value)) {
            return hashtable.get(value).getHandle();
        }
        return null;
    }

}


/**
 * @class CacheAuthValue
 *
 * Clase POJO para almacenar los datos en dspace de una autoridad
 *
 */
class CacheAuthValue
{
    /**
     * handle relacionado con la autoridad
     */
    private String handle;

    /**
     * valor de la autoridad
     */
    private String value;

    /**
     * número de coincidencias
     */
    private int numMatches;


    /**
     * Constructor
     *
     * @param value valor de la autoridad
     * @param handle handle de la autoridad
     */
    public CacheAuthValue(String value, String handle)
    {
        this.numMatches = 1;
        this.handle = handle;
        this.value = value;
    }

    /**
     * Devuelve el valor
     *
     * @return el valor
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Devuelve el handle
     *
     * @return el handle
     */
    public String getHandle()
    {
        return handle;
    }

    /**
     * Devuelve el número de coincidencias
     *
     * @return el número de coincidencias
     */
    public int getNumMatches()
    {
        return numMatches;
    }

    /**
     * Incrementa en uno el número de coincidencias
     *
     */
    public void incNumMatches()
    {
        numMatches++;
    }
}
