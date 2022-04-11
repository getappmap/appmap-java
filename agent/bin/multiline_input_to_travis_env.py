#!/usr/bin/env python

# converts multiline stdin into a form suitable for setting env vars in Travis UI
# see https://github.com/travis-ci/travis-ci/issues/7715#issuecomment-362536708

import sys

body=sys.stdin.read() 
body = body.replace('\n','\\n')
body = '"$( echo -e '+"'"+body+"'"+')"'
print(body)
