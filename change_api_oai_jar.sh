#!/bin/sh

my_dir=$(dirname $0)
my_name=$(basename $0)

help_sub()
{
    echo "Use: "$my_name": -f file_api_oai_jar-c crosswalk_class [ -s show_list
]" >&2
    exit 1
}

test $# -eq 0 && help_sub

OAIApiJar=""
CrosswalkClass=""
show_list=0

while [ -n "$*" ];
do
    case $1 in
        "-f" )
            shift
            OAIApiJar="$1"
            ;;
        "-c" )
            shift
            CrosswalkClass="$1"
            ;;
        "-s" )
            show_list=1
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



[[ -z "$OAIApiJar" || ! -f "$OAIApiJar" ]] && echo "File $OAIApiJar
does no exist" && exit 1

[[ -z "$CrosswalkClass" || ! -f "$CrosswalkClass" ]] && echo "File
$CrosswalkClass
does no exist" && exit 1

jar_cmd=$( which jar 2>/dev/null)
[[ -z "$jar_cmd" || ! -x "$jar_cmd" ]] && echo "Command jar does no exist" &&
exit 1

zip_cmd=$( which zip 2>/dev/null)
[[ -z "$zip_cmd" || ! -x "$zip_cmd" ]] && echo "Command zip does no exist" &&
exit 1

unzip_cmd=$( which unzip 2>/dev/null)
[[ -z "$unzip_cmd" || ! -x "$unzip_cmd" ]] && echo "Command unzip does no
exist"\
&& exit 1



if [ $show_list -eq 0 ]; then
    test -d /tmp/europeana && rm -rf /tmp/europeana

    CrosswalkClassName=$(basename $CrosswalkClass)

    mkdir -p /tmp/europeana/org/dspace/app/oai\
    && cp $CrosswalkClass /tmp/europeana/org/dspace/app/oai &&\
    $jar_cmd uf "$OAIApiJar" -C /tmp/europeana "org/dspace/app/oai/$CrosswalkClassName"\
    && echo "File $OAIApiJar changed successfully"

    test -d /tmp/europeana && rm -rf /tmp/europeana
else
    $unzip_cmd -l "$OAIApiJar"
fi
