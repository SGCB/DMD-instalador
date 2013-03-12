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
