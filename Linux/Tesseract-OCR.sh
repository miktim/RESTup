#!/bin/bash
# 2014-2015 miktim@mail.ru
# usage: Tesseract-OCR indir outdir language
for srcfile in $1*
do
  echo "$srcfile" >&2
  if [ -d "${srcfile}" ]; then # directory?
    continue
  fi
  fname=${srcfile##*/}
  tesseract "$srcfile" "$2""${fname%.*}" -l $3
  exit_code=$?
  if [ $exit_code -ne 0 ]; then 
    exit $exit_code 
  fi
# add MS utf-8 signature (BOM)
#  printf "\xef\xbb\xbf" | cat - $2"${fname%.*}"'.txt' > $2'signature.tmp'
#  mv $2'signature.tmp' $2"${fname%.*}"'.txt'
done
exit $?