#!/usr/bin/env python

import sys

# this script exists entirely because it's easier to handle quoting issues with
# inline-YML in Python than in BASH (and the associated bizareness of string literals there)

print  """
{arguments: "-zipkin.web.port=:$PORT -zipkin.web.rootUrl=/ -zipkin.web.query.dest=%s:80"}
""".strip() % sys.argv[1]