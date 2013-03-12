package org.dspace.installer_edm;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.terminal.Terminal;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @class InstallerEDMDisplayImpl
 *
 * Implementación de la visualización por pantalla de los mensajes.
 * En este caso se visualiza mediante consola.
 * Implementa la interfaz {@link InstallerEDMDisplay}
 *
 */
public class InstallerEDMDisplayImpl implements InstallerEDMDisplay
{
    /**
     * indica si se va a usar la librería com.googlecode.lanterna.TerminalFacade para mostrar por pantalla,
     * Actualmente no implementado.
     */
    private boolean isTerminal = false;

    /**
     * Objeto para com.googlecode.lanterna.TerminalFacade
     */
    private Terminal terminal;

    /**
     * nombre canónico donde buscar lo mensajes
     */
    private static final String nameFileDisplayMessages = "org.dspace.installer_edm.messages";

    /**
     * propiedades con los mensajes leídos del archivo
     */
    private Properties properties = new Properties();

    /**
     * objeto de {@link LoadFileMessages} que carga los archivos de mensajes
     */
    private LoadFileMessages loadFileMessages;


    /**
     * Constructor que carga el archivo con idioma por defecto
     */
    public InstallerEDMDisplayImpl()
    {
        this(null);
    }

    /**
     * Constructor que carga los mensajes para un idioma y establece el terminal si lo hubiere
     *
     * @param language idioma de los mensajes
     */
    public InstallerEDMDisplayImpl(String language)
    {
        loadFileDisplayMessages(language);
        if (isTerminal) {
            setTerminal();
        }
    }

    /**
     * muestra el título de un paso. En el archivo la propiedad está como: stage=
     *
     * @param stage paso actual
     */
    @Override
    public void showTitle(int stage) {
        String key = Integer.toString(stage);
        if (properties.containsKey(key)) {
            if (!isTerminal) System.out.println(properties.getProperty(key));
        }
    }

    /**
     * muestra el menú de un paso. En el archivo la propiedad está como: stage.menu=
     * cada elemento del menú está separa por un |
     *
     * @param stage paso actual
     */
    @Override
    public void showMenu(int stage)
    {
        String key = Integer.toString(stage) + ".menu";
        if (properties.containsKey(key)) {
            String[] lista = properties.getProperty(key).split("\\|");
            if (lista.length > 0)
                for (String val : lista) {
                    System.out.println(val);
                }
        }
    }

    /**
     * recoge el mensaje de un paso con código. En el archivo la propiedad está como: stage.code=
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @return mensaje
     */
    @Override
    public String getQuestion(int stage, String code)
    {
        String key = Integer.toString(stage) + "." + code;
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        return "";
    }

    /**
     * recoge el mensaje de un paso con código y con argumentos. En el archivo la propiedad está como: stage.code= y se sustituye el patrón #?# por cada uno de los argumentos
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @param args argumentos
     * @return mensaje
     */
    @Override
    public String getQuestion(int stage, String code, String[] args)
    {
        if (args == null || args.length == 0) return getQuestion(stage, code);
        String text = getQuestion(stage, code);
        for (String arg : args) {
            if (arg == null) arg = "";
            arg = arg.replaceAll("\\\\[^\\\\]*",  " ");
            arg = arg.replaceAll("\\(",  "\\(");
            arg = arg.replaceAll("\\)",  "\\)");
            arg = arg.replaceAll("\\$",  "\\$");
            text = text.replaceFirst("#\\?#", arg);
        }
        return text;
    }

    /**
     * muestra el mensaje de un paso con código y con argumentos. En el archivo la propiedad está como: stage.code= y se sustituye el patrón #?# por cada uno de los argumentos
     *
     * @param stage paso actual
     * @param code código del mensaje
     * @param args argumentos
     */
    @Override
    public void showQuestion(int stage, String code, String[] args)
    {
        if (args == null || args.length == 0) showQuestion(stage, code);
        String text = getQuestion(stage, code, args);
        if (text != null && !text.isEmpty()) {
            if (!isTerminal) System.out.println(text);
        }
    }

    /**
     * muestra el mensaje de un paso con código. En el archivo la propiedad está como: stage.code=
     *
     * @param stage paso actual
     * @param code código del mensaje
     */
    @Override
    public void showQuestion(int stage, String code)
    {
        String text = getQuestion(stage, code);
        if (!text.isEmpty()) {
            if (!isTerminal) System.out.println(text);
        }
    }

    /**
     * muestra el mensaje pasado como argumento
     *
     * @param message mensaje a mostrar
     */
    @Override
    public void showMessage(String message)
    {
        if (!isTerminal) System.out.println(message);
    }

    /**
     * muestra un salto de línea
     */
    @Override
    public void showLn()
    {
        if (!isTerminal) System.out.println();
    }

    /**
     * muestra progreso de una acción
     *
     * @param prog carácter a mostrar por cada iteración
     */
    @Override
    public void showProgress(char prog)
    {
        if (!isTerminal) System.out.print(prog);
    }


    /**
     * asigna el terminal y lo inicializa
     */
    public void setTerminal()
    {
        terminal = TerminalFacade.createTerminal(Charset.forName("UTF8"));
        terminal.enterPrivateMode();
        //TerminalSize screenSize = terminal.getTerminalSize();
        terminal.clearScreen();
    }

    /**
     * sale del terminal
     */
    public void exitTerminal()
    {
        if (isTerminal) terminal.exitPrivateMode();
    }

    /**
     * asigna comprobación de terminal
     *
     * @param isTerminal indica si hay terminal
     */
    public void setIsTerminal(boolean isTerminal)
    {
        this.isTerminal = isTerminal;
    }

    /**
     * Devuelve el terminal
     *
     * @return objeto terminal
     */
    public boolean getIsTerminal()
    {
        return this.isTerminal;
    }

    /**
     * Recarga un archivo de mensajes para un idioma
     *
     * @param language idioma del archivo
     */
    @Override
    public void reloadFileDisplayMessages(String language)
    {
        if (!properties.isEmpty()) properties.clear();
        loadFileDisplayMessages(language);
    }

    /**
     * Carga un archivo de mensajes para un idioma
     *
     * @param language idioma del archivo
     */
    private void loadFileDisplayMessages(String language)
    {
        loadFileMessages = new LoadFileMessages(nameFileDisplayMessages, language);
        for (Enumeration<String> e = loadFileMessages.getKeys(); e.hasMoreElements();) {
            String key = e.nextElement();
            String val = loadFileMessages.getString(key);
            properties.setProperty(key, val);
        }
    }
}
