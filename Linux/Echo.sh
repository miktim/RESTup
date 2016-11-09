#!/bin/bash

cd $1; find . -name "$3" | cpio -pdm $2
exit $?