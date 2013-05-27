@echo off

:: d:\usuario\programacion\batch\edm\install.bat -d d:\usuario\programacion\batch\edm -b d:\usuario\programacion\batch\edm -s 2 -v
:: c:\tmp>install.bat -d c:\dspace_171 -l es_ES -s 0 -v -g -t "c:\Program Files\Apache Software Foundation\Tomcat 6.0" -j "c:\Program Files\Java\jdk1.7.0_21"

setlocal EnableDelayedExpansion

set my_name=%~nx0
set my_dir=%~dp0


set help=0
set verbose=0
set debug=0
set dir_space_runtime=
set tomcat_base=
set step=1
set language=""
set java_cmd_param=""

set argC=0
for %%x in (%*) do set /A argC+=1

if %argC% == 0 (
    call:help_sub
    goto END
)
goto LoopArgs

:DeQuote
    FOR %%G IN (%*) DO (
        SET DeQuote.Variable=%%G
        CALL SET DeQuote.Contents=%%!DeQuote.Variable!%%
        IF [!DeQuote.Contents:~0^,1!]==[^"] (
            IF [!DeQuote.Contents:~-1!]==[^"] (
                SET DeQuote.Contents=!DeQuote.Contents:~1,-1!
            ) ELSE (GOTO :EOF no end quote)
        ) ELSE (GOTO :EOF no beginning quote)
        SET !DeQuote.Variable!=!DeQuote.Contents!
        SET DeQuote.Variable=
        SET DeQuote.Contents=
    )
Goto :EOF

:LoopArgs
    if "%1" == "" goto ContinueArgs
    set segundo=%2
    CALL :dequote segundo
    if /I "%1" == "-d" (
        if not "!segundo!" == "" (
            set dir_space_runtime=!segundo!
            echo.Ruta Dspace: !dir_space_runtime!
            shift
        )
    ) else (
        if /I "%1" == "-j" (
            if not "!segundo!" == "" (
                set java_cmd_param=!segundo!
                echo.Ruta Java: !java_cmd_param!
                shift
            )
        ) else (
            if /I "%1" == "-t" (
                if not "!segundo!" == "" (
                    set tomcat_base=!segundo!
                    echo.Ruta Tomcat: !tomcat_base!
                    shift
                )
            ) else (
                if /I "%1" == "-l" (
                    if not "!segundo!" == "" (
                        set language=!segundo!
                        echo.Idioma: !language!
                        shift
                    )
                ) else (
                    if /I "%1" == "-s" (
                        if not "!segundo!" == "" ( 
                            set /A step=!segundo!
                            echo.Paso: !step!
                            shift
                        )
                    ) else (
                        if /I "%1" == "-v" (
                            set /A verbose=1
                            echo.Verbose
                        ) else (
                            if /I "%1" == "-g" ( 
                                set /A debug=1
                                echo.Debug
                            )
                        )
                    )
                )
            )
        )
    )
    shift
    goto LoopArgs
:ContinueArgs


:MYDIRNOK
    set or_=
    if "%my_dir%" == "" set or_=true
    if not exist "%my_dir%" set or_=true
    if defined or_ (
        set my_dir=""
        set response=
        set /P response=Directory running this install: || set response=fgsfgsdfg
        if not "!response!" == "" ( set my_dir=!response! )
        goto MYDIRNOK
    ) else (
        if exist "%my_dir%\lib" if exist "%my_dir%\packages" goto MYDIROK
        echo.
        echo. There is no lib or packages directory in %my_dir%
        set my_dir=""
        goto MYDIRNOK
    )
:MYDIROK


:DSRDIRNOK
    set or_=
    if "%dir_space_runtime%" == "" set or_=true
    if not exist "%dir_space_runtime%" set or_=true
    if defined or_ (
        set dir_space_runtime=""
        set response=
        set /P response=Directory where dspace is deployed: || set response=fgsfgsdfg
        if not "!response!" == "" (
            set dir_space_runtime=!response!
        )
        goto DSRDIRNOK
    ) else (
        if exist "%dir_space_runtime%\lib" if exist "%dir_space_runtime%\config" goto DSRDIROK
        echo.
        echo. There is no lib or config directory in %dir_space_runtime%
        set dir_space_runtime=""
        goto DSRDIRNOK
    )
:DSRDIROK


:TOMCATDIRNOK
    set or_=
    if "%tomcat_base%" == "" set or_=true
    if not exist "%tomcat_base%" set or_=true
    if defined or_ (
        set tomcat_base=""
        set response=
        set /P response=Directory Base Tomcat: || set response=fgsfgsdfg
        if not "!response!" == "" (
            set tomcat_base=!response!
        )
        goto TOMCATDIRNOK
    ) else (
        goto TOMCATDIROK
    )
:TOMCATDIROK


:STEPNOK
    set or_=
    if "%step%" == "" set or_=true
    if %step% gtr 8 set or_=true
    if %step% lss 0 set or_=true
    if defined or_ (
        set step=0
        set response=
        set /P response=Step 0 1,2,3,4,5,6,7,8: || set response=1000
        if not "!response!" == "" (
            set /A step=!response!
        )
        goto STEPNOK
    ) else (
        goto STEPOK
    )
:STEPOK


set "JARS="
for %%f in (%my_dir%\lib\*.jar) do set "JARS=!JARS!%%f;"

set "JARS2="
for %%f in (%dir_space_runtime%\lib\*.jar) do set "JARS2=!JARS2!%%f;"

set found=
set prog=where.exe
for %%i in (%path%) do if exist %%i\%prog% set found=%%i\%prog%
set "java_where="
if not "%found%" == "" (
    for /f "delims=" %%a in ('%found% java') do set java_where=%%a
)

set or_=
set java_home_var=%JAVA_HOME%
if "%java_home_var%" == "" (
    set or_=true
) else (
    set java_home_var=!java_home_var!\bin\java.exe
    if not exist "!java_home_var!" (
        set or_=true
    )
)

if defined or_ (
    set java_home_var=
    set or_=
    if "!java_where!" == "" set or_=true
    if not exist "!java_where!" set or_=true
    if not defined or_ (
        set java_home_var="!java_where!"
    )
)

:JAVANOK
    set or_=
    if "!java_cmd_param!" == "" set or_=true
    if not defined or_ (
        if not exist "!java_cmd_param!" set or_=true
        if not defined or_ (
            pushd "!java_cmd_param!" 2>NUL
            if errorlevel 1 ( 
                set errorlevel=0
            ) else ( 
                set or_=true
                popd
            )
        )
    )
    if defined or_ (
        set java_cmd_param=""
        set response=
        set /P response=Path of java command !java_home_var!: || set response=!java_home_var!
        if not "!response!" == "" (
            set java_cmd_param=!response!
        ) else (
            set java_cmd_param=!java_home_var!
            goto JAVAOK
        )
        goto JAVANOK
    ) else (
        goto JAVAOK
    )
:JAVAOK

echo.To be able to see properly the accented characters the code page is going to be changed to 65001 and you have to change manually the font from the console properties to Lucida console.

(
chcp 65001

:: echo.!java_cmd_param! -cp %JARS%%JARS2%InstallerEDM.jar;.;"%dir_space_runtime%\config" org.dspace.installer_edm.InstallerEDM %*
"!java_cmd_param!" -Dfile.encoding=UTF-8 -cp %JARS%%JARS2%InstallerEDM.jar;.;"%dir_space_runtime%\config" org.dspace.installer_edm.InstallerEDM %*

chcp 850
)

goto END


:help_sub
    echo.
    echo. "Use: "%my_name%": [-d dir_space_runtime] [-j java_command] [-h] [-l language] [-s step] [-t dir_tomcat_base] [-v] [-g]"
    goto END
GOTO:EOF

:END

endlocal
