@ECHO OFF

IF "%1" == "" (
    IF "%2" == "" (
        ECHO "Usage: %0 path\to\input.boa output-dir [options]"
        EXIT /B 1
    )
)

SET input=%1
SET output=%2

SHIFT
SHIFT

IF NOT EXIST "%input%" (
    ECHO "input '%input%' is not a file"
    EXIT /B 2
)

REM need to convert this to batch script
REM if [ -d $2 ]; then
REM     read -n 1 -p "output directory '$2' exists - delete? [Y/n] " yn
REM     ECHO ""
REM
REM     yn=`echo $yn | tr '[:upper:]' '[:lower:]'`
REM
REM     if [[ $yn =~ ^(y| ) ]] | [ -z $yn ]; then
REM         rm -Rf $2
REM     else
REM         ECHO "Please remove or provide a different output directory."
REM         EXIT /B 3
REM     fi
REM fi

"%~dp0\boa.bat" -e -d dataset/ -i %input% -o %output% %*
