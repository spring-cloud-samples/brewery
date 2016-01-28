#!/usr/bin/env python

import sys

# this script exists entirely because it's easier to handle quoting issues with
# inline-YML in Python than in BASH (and the associated bizareness of string literals there)

print  """
{arguments: "-spring.cloud.zookeeper.connectString=%s:2181"}
""".strip() % sys.argv[1]