package org.dspace.installer_edm;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.terminal.Terminal;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: salvazm-adm
 * Date: 15/01/13
 * Time: 9:44
 * To change this template use File | Settings | File Templates.
 */
public class InstallerEDMDisplayImpl implements InstallerEDMDisplay
{
    private boolean isTerminal = false;
    private Terminal terminal;
    private static final String nameFileDisplayMessages = "org.dspace.installer_edm.messages";
    private Properties properties = new Properties();
    private LoadFileMessages loadFileMessages;

    public InstallerEDMDisplayImpl()
    {
        this(null);
    }

    public InstallerEDMDisplayImpl(String language)
    {
        loadFileDisplayMessages(language);
        if (isTerminal) {
            setTerminal();
        }
    }

    @Override
    public void showTitle(int stage) {
        String key = Integer.toString(stage);
        if (properties.containsKey(key)) {
            if (!isTerminal) System.out.println(properties.getProperty(key));
        }
    }

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


    @Override
    public String getQuestion(int stage, String code)
    {
        String key = Integer.toString(stage) + "." + code;
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        }
        return "";
    }

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

    @Override
    public void showQuestion(int stage, String code, String[] args)
    {
        if (args == null || args.length == 0) showQuestion(stage, code);
        String text = getQuestion(stage, code, args);
        if (text != null && !text.isEmpty()) {
            if (!isTerminal) System.out.println(text);
        }
    }

    @Override
    public void showQuestion(int stage, String code)
    {
        String text = getQuestion(stage, code);
        if (!text.isEmpty()) {
            if (!isTerminal) System.out.println(text);
        }
    }


    @Override
    public void showMessage(String message)
    {
        if (!isTerminal) System.out.println(message);
    }

    @Override
    public void showLn()
    {
        if (!isTerminal) System.out.println();
    }

    @Override
    public void showProgress(char prog)
    {
        if (!isTerminal) System.out.print(prog);
    }


    public void setTerminal()
    {
        terminal = TerminalFacade.createTerminal(Charset.forName("UTF8"));
        terminal.enterPrivateMode();
        //TerminalSize screenSize = terminal.getTerminalSize();
        terminal.clearScreen();
    }

    public void exitTerminal()
    {
        if (isTerminal) terminal.exitPrivateMode();
    }

    public void setIsTerminal(boolean isTerminal)
    {
        this.isTerminal = isTerminal;
    }

    public boolean getIsTerminal()
    {
        return this.isTerminal;
    }

    @Override
    public void reloadFileDisplayMessages(String language)
    {
        if (!properties.isEmpty()) properties.clear();
        loadFileDisplayMessages(language);
    }

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
