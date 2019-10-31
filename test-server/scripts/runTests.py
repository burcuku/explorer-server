#!/usr/bin/python
import os
import sys
import time
from os import path
from subprocess import call

def main(numtests):
    os.chdir("..")
    start_all = time.time()
    for i in range(1, int(numTests)+1):
        print("Running test %s" % i)
        startB = time.time()

        seed = i + 12345678
        call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=explorer.ServerMain", "-Dexec.args=\"seed %s\" " % str(seed)), shell=True)

        endB = time.time()
        elapsedSec = endB - startB
        print("All Seconds for test %s: %s" % (i, elapsedSec))
        print("All Minutes for test %s: %s" % (i, elapsedSec / 60))

    endAll = time.time()
    elapsedSec = endAll - start_all
    print("All Seconds for all tests: %s" % elapsedSec)
    print("All Minutes for all tests: %s" % (elapsedSec / 60))


if __name__ == '__main__':
    numtests = 1
    if len(sys.argv) == 2:
        numTests = sys.argv[1]

    main(numTests)
