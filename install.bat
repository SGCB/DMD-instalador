@echo off

:: d:\salva\programacion\batch\edm\install.bat -d d:\salva\programacion\batch\edm -b d:\salva\programacion\batch\edm -s 2 -v

setlocal EnableDelayedExpansion

set my_name=%~nx0
set my_dir=%~dp0


set help=0
set verbose=0
set dir_space_runtime=""
set tomcat_base=""
set step=1

set argC=0
for %%x in (%*) do set /A argC+=1

if %argC% == 0 call:help_sub

:LoopArgs
    if "%1" == "" goto ContinueArgs
    if /I "%1" == "-d" (
        if not "%2" == "" ( 
            set dir_space_runtime=%2%
        )
    ) else (
        if /I "%1" == "-b" (
            if not "%2" == "" ( 
                set tomcat_base=%2%
            )
        ) else (
            if /I "%1" == "-s" ( 
                if not "%2" == "" ( set /A step=%2% )
            ) else (
                if /I "%1" == "-v" ( set /A verbose=1 )
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
        set /P response=Directory running this install: 
        if not "%response%" == "" ( set my_dir=!response! )
        goto MYDIRNOK
    ) else (
        if exist "%my_dir%\lib" if exist "%my_dir%\packages" goto MYDIROK
        echo.
        echo. There is no lib or packages directory in %my_dir%
        goto END
    )
:MYDIROK


:DSRDIRNOK
    set or_=
    if "%dir_space_runtime%" == "" set or_=true
    if not exist "%dir_space_runtime%" set or_=true
    if defined or_ (
        set dir_space_runtime=""
        set response=
        set /P response=Directory where dspace is deployed: 
        if not "%response%" == "" ( 
            set dir_space_runtime=!response!
        )
        goto DSRDIRNOK
    ) else (
        if exist "%dir_space_runtime%\lib" if exist "%dir_space_runtime%\config" goto DSRDIROK
        echo.
        echo. There is no lib or config directory in %dir_space_runtime%
        goto END
    )
:DSRDIROK


:TOMCATDIRNOK
    set or_=
    if "%tomcat_base%" == "" set or_=true
    if not exist "%tomcat_base%" set or_=true
    if defined or_ (
        set tomcat_base=""
        set response=
        set /P response=Directory Base Tomcat: 
        if not "%response%" == "" ( 
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
    if %step% gtr 5 set or_=true
    if %step% lss 1 set or_=true
    if defined or_ (
        set step=1
        set response=
        set /P response=Step 1,2,3,4,5: 
        if not "%response%" == "" ( 
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

java -cp %JARS%%JARS2%out\production\instalador_edm\InstallerEDM.jar;out\production\instalador_edm;%dir_space_runtime%\config org.dspace.installer_edm.InstallerEDM %*


goto END


:help_sub
    echo.
    echo. "Use: "%my_name%": [-d dir_space_runtime] [-h] [-s step] [-t dir_tomcat_base] [-v]"
    goto END
GOTO:EOF

:END

endlocal