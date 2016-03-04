#!/bin/bash
# 2014-2015 miktim@mail.ru
# usage: Office2x indir outdir PDF | HTML | MSO97 | OOXML
# See also: http://www.commandlinefu.com/commands/view/11692/commandline-document-conversion-with-libreoffice
exit_code=0
convTo=$(echo $3 | tr '[:upper:]' '[:lower:]')
param=$convTo
for fname in $1*
do 
  echo "$fname" >&2
  if [ -d "${fname}" ]; then #directory?
    continue
  fi
  fext=${fname##*.}
  fext=$(echo $fext | tr '[:upper:]' '[:lower:]')
#  echo $fext >&2
  if [ $param = mso97 ]; then
    convTo=doc
    case $fext in
       docx|odt|html) convTo=doc ;;
       xlsx|ods) convTo=xls ;;
       pptx|odp) convTo=ppt ;;
    esac
  fi
  if [ $param = ooxml ]; then
    convTo=docx
    case $fext in
       doc|odt|html) convTo=docx ;;
       xls|ods) convTo=xlsx ;;
       ppt|odp) convTo=pptx ;;
    esac
  fi
  echo $convTo >&2
  soffice --headless --convert-to $convTo --outdir "$2" "$fname"
  exit_code=$?
  if [ $exit_code -ne 0 ]; then
    exit $exit_code
  fi
done
exit $?