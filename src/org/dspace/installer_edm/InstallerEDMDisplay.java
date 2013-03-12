package org.dspace.installer_edm;

/**
 * @class InstallerEDMDisplay
 *
 * Interfaz para mostrar los mensajes de los archivos messages
 *
 */
public interface InstallerEDMDisplay
{

    /**
     * muestra el título de un paso. En el archivo la propiedad está como: stage=
     *
     * @param stage paso actual
     */
    public void showTitle(int stage);

    /**
     * muestra el menú de un paso. En el archivo la propiedad está como: stage.menu=
     *
     * @param stage paso actual
     */
    public void showMenu(int stage);

    /**
     * recoge el mensaje de un paso con código. En el archivo la propiedad está como: stage.code=
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @return mensaje
     */
    public String getQuestion(int stage, String code);

    /**
     * recoge el mensaje de un paso con código y con argumentos. En el archivo la propiedad está como: stage.code= y se sustituye el patrón #?# por cada uno de los argumentos
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @param args argumentos
     * @return mensaje
     */
    public String getQuestion(int stage, String code, String[] args);

    /**
     * muestra el mensaje de un paso con código y con argumentos. En el archivo la propiedad está como: stage.code= y se sustituye el patrón #?# por cada uno de los argumentos
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @param args argumentos
     */
    public void showQuestion(int stage, String code, String[] args);

    /**
     * muestra el mensaje de un paso con código. En el archivo la propiedad está como: stage.code=
     *
     * @param stage paso actual
     * @param code código del mensaje
     */
    public void showQuestion(int stage, String code);

    /**
     * muestra el mensaje pasado como argumento
     *
     * @param message mensaje a mostrar
     */
    public void showMessage(String message);

    /**
     * muestra un salto de línea
     */
    public void showLn();

    /**
     * muestra progreso de una acción
     *
     * @param prog carácter a mostrar por cada iteración
     */
    public void showProgress(char prog);

    /**
     * recarga el archivo de mensajes para un idioma
     *
     * @param language idioma del archivo
     */
    public void reloadFileDisplayMessages(String language);

}
