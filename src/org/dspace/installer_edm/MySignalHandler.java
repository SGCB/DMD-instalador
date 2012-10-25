package org.dspace.installer_edm;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.Observable;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 25/10/12
 * Time: 9:39
 * To change this template use File | Settings | File Templates.
 */
class MySignalHandler extends Observable implements SignalHandler
{
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

    public void handle( final Signal signal )
    {
        setChanged();
        notifyObservers( signal );
    }
}
