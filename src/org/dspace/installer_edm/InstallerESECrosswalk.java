package org.dspace.installer_edm;

/**
 * @class InstallerESECrosswalk
 *
 * Clase para instalar el plugin de ESECrosswalk de java en dspace para mostrar en OAI los ítems en formato ESE.
 *
 * Se copia el archivo ESECrosswalk.java al directorio de trabajo del instalador,
 * se cambian en el fuente del java, se compila y se genera un class que se añade al jar de la api de oai de dspace.
 * Se copia oaicat.properties al directorio de trabajo del instalador y se modifica para añadir el crosswalk para ese
 *
 */
public class InstallerESECrosswalk extends InstallerCrosswalk
{

    /**
     * Constructor
     *
     * @param currentStepGlobal paso actual
     * @param crosswalkName nombre del crosswalk
     */
    public InstallerESECrosswalk(Integer currentStepGlobal, String crosswalkName)
    {
        super(currentStepGlobal.intValue(), crosswalkName);
    }

    @Override
    public void configureCrosswalk()
    {

    }
}
