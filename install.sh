#!/bin/sh

# ./install.sh -v -d /home/salzaru/Download/dspace/ -s 1 -t /usr/local/apache-tomcat-6.0/

os=$(uname)

if [ "$os" = "Linux" ]; then
    myshell=$(ps -o "%c" -p $$ 2>/dev/null | tail -1)
else
    myshell=$(ps -o "comm" -p $$ | tail -1)
fi
test -z "$myshell" && myshell=$(basename $SHELL)


my_name=$(basename $0)
my_dir=$(dirname $0)
file_lock="installEDM.lock"

red=''
RED=''
green=''
GREEN=''
yellow=''
YELLOW=''
blue=''
BLUE=''
NC=''

if [ "$myshell" = "sh" ]; then
    red='\033[31m'
    RED='\033[31m'
    green='\033[32m'
    GREEN='\033[32m'
    yellow='\033[33m'
    YELLOW='\033[33m'
    blue='\033[34m'
    BLUE='\033[34m'
    NC='\033[0m'
else
    red='\e[0;31m'
    RED='\e[1;31m'
    green='\e[0;32m'
    GREEN='\e[1;32m'
    yellow='\e[0;33m'
    YELLOW='\e[1;33m'
    blue='\e[0;34m'
    BLUE='\e[1;34m'
    NC='\e[0m'
fi


chomp()
{
    local char=${2:-$'\n'}
    eval "$1=\${${1}%$char}"
}


flush_stdin()
{
    stty min 0 time 1 -icanon
    while [ -n "$(dd bs=1 count=1 2>/dev/null)" ]
    do
        continue
    done
    stty min 1 time 0 icanon
}


message_sub()
{
    MESSAGE=$1

    if [ "$myshell" = "sh" ]; then
        printf "$MESSAGE"
        echo ""
    else
        echo -e $MESSAGE
    fi
}



help=0
verbose=0
dir_space_runtime=""
tomcat_base=""
step=1
language=""


clean_up ()
{
    stty echo
    test $verbose -eq 1 && echo "Cleaning up"
    test -f "$file_lock" && rm $file_lock
    local CPIDS="$(pgrep -P $$ 2>/dev/null)"
    (test -n "$CPIDS" && sleep 1 && kill -15 $CPIDS 2>/dev/null &);
    wait
}


help_sub()
{
    message_sub "${BLUE}Use: "$my_name": [-d dir_space_runtime] [-h] [-l language] [-s step] [-t dir_tomcat_base] [-v]${NC}" >&2
    clean_up
    exit 2
}


test $# -eq 0 && help_sub

while [ -n "$*" ];
do
    case $1 in
        "-d" )
            shift
            dir_space_runtime=$1
            ;;
        "-l" )
            shift
            language=$1
            ;;
        "-s" )
            shift
            step=$1
            ;;
        "-t" )
            shift
            tomcat_base=$1
            ;;
        "-v" )
            verbose=1
            ;;
        "-h" | "?" )
            help_sub
            ;;
        * )
            help_sub
            ;;
    esac
    shift
done

dir_ok=0

until [ $dir_ok -eq 1 ]; do
    if [ -z "$my_dir" -o ! -d "$my_dir" ]; then
        my_dir=""
        until [ -n "$my_dir" -a -d "$my_dir" ]; do
            message_sub "${YELLOW}Directory running this install: ${NC}"
            flush_stdin
            read my_dir
        done
    fi
    if [ -d "$my_dir/lib" -a -d "$my_dir/packages" ]; then
        dir_ok=1
    else
        message_sub "${YELLOW}There is no lib or packages directory in $my_dir${NC}"
    fi
done

file_lock=$my_dir"/"$file_lock
if [ -f $file_lock ]; then
    process=$(ps -ef | grep -m 1 "$my_name" | awk -F " " '{print $2}')
    if [ -n "$process" -a "$process" = $(cat $file_lock) ]; then
        message_sub "${RED}There's already a process install${NC}" >&2
        exit 1
    else
        echo $$ > $file_lock
    fi
else
    echo $$ > $file_lock
fi



dir_ok=0

until [ $dir_ok -eq 1 ]; do
    if [ -z "$dir_space_runtime" -o ! -d "$dir_space_runtime" ]; then
        dir_space_runtime=""
        until [ -n "$dir_space_runtime" -a -d "$dir_space_runtime" ]; do
            message_sub "${YELLOW}Directory where dspace is deployed: ${NC}"
            flush_stdin
            read dir_space_runtime
        done
    fi
    if [ -d "$dir_space_runtime/lib" -a -d "$dir_space_runtime/config" ]; then
        dir_ok=1
    else
        test ! -d "$dir_space_runtime/lib" && message_sub "${YELLOW}There is no lib directory in $dir_space_runtime${NC}"
        test ! -d "$dir_space_runtime/config" && message_sub "${YELLOW}There is no config directory in $dir_space_runtime${NC}"
    fi
done


if [ -z "$tomcat_base" -o ! -d "$tomcat_base" ]; then
    tomcat_base=""
    until [ -n "$tomcat_base" -a -d "$tomcat_base" ]; do
        message_sub "${YELLOW}There is no Tomcat Base Directory specified correctly. Tomcat Base Directory: ${NC}"
        flush_stdin
        read tomcat_base
    done
fi

if [ -z "$step" -o $(echo "$step" | grep -q "^[012345]$"; echo $?) -eq 1 ]; then
    step=""
    until [ -n "$step" -a $(echo "$step" | grep -q "^[012345]$"; echo $?) -eq 0 ]; do
        message_sub "${YELLOW}Step must be 0 or 1 or 2 or 3 or 4 or 5. Step: ${NC}"
        flush_stdin
        read step
    done
fi


arg_str=""

test -n "$language" && arg_str="$arg_str -l $language"
test $verbose -eq 1 && arg_str="$arg_str -v"


JARS=$(echo $my_dir/lib/*.jar | sed 's/ /\:/g')
JARS2=$(echo $dir_space_runtime/lib/*.jar | sed 's/ /\:/g')

java -cp $JARS:$JARS2:out/production/instalador_edm/InstallerEDM.jar:out/production/instalador_edm:$dir_space_runtime/config org.dspace.installer_edm.InstallerEDM -d $dir_space_runtime -t $tomcat_base -s $step $arg_str

clean_up
exit 0
