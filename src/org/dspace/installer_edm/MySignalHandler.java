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

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.Observable;

/**
 * @class MySignalHandler
 *
 * Clase para capturar las interrupciones o señales por teclado
 *
 */
class MySignalHandler extends Observable implements SignalHandler
{
    /**
     * handleSignal se captura la señal y se procesa
     *
     * @param signalName nombre de la señal capturada
     * @throws IllegalArgumentException
     */
    public void handleSignal( final String signalName ) throws IllegalArgumentException
    {
        try {
            Signal.handle(new Signal(signalName), this);
        }
        catch( IllegalArgumentException x ) {
            throw x;
        }
        catch( Throwable x ) {
            throw new IllegalArgumentException( "Signal unsupported: " + signalName, x );
        }
    }

    /**
     * Procesamiento de la señal.
     * Se notifica a la clase en ejecución actual.
     *
     * @param signal objeto Signal con la señal
     */
    public void handle( final Signal signal )
    {
        setChanged();
        notifyObservers( signal );
    }
}
