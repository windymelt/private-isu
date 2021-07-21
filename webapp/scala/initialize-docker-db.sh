#!/bin/bash

set -ex

if [ -f dump.sql.bz2 ]; then
  GENUINE_SHA256SUM="db27f63989a02babb6d47b1eb0ac1831d0f6d53f7de724407054feb2ff1d36c9"
  DIGEST=$(openssl dgst -sha256 -r dump.sql.bz2 | cut -c1-64 | tr -d $'\n')
  if [ $DIGEST != $GENUINE_SHA256SUM ]; then
    rm dump.sql.bz2
    wget -q https://github.com/catatsuy/private-isu/releases/download/img/dump.sql.bz2
  fi
  else
    wget -q https://github.com/catatsuy/private-isu/releases/download/img/dump.sql.bz2
fi
cd ../
bzcat scala/dump.sql.bz2 | pv -s 2510161810 | docker compose run --rm -T mysql mysql -h mysql -u root --password="root"