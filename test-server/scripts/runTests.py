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

    # create mutations
    call("mvn {0} {1}".format("exec:java", "-Dexec.mainClass=explorer.MutatorCreatorMain"), shell=True)

    startM = time.time()
    # run mutations
    while not isFileEmpty("mutations"):
        call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=explorer.ServerMain", "-Dexec.args=\"seed %s\" " % str(seed)), shell=True)
    endM = time.time()
    elapsedM = endM - startM
    elapsedSec2 = endM - start_all
    print("All Seconds for mutations: %s" % elapsedM)
    print("All Seconds for all: %s" % elapsedSec2)

def isFileEmpty(filename):
    with open(filename, 'r') as f:
        lines = [len(line.rstrip('\n')) == 0 for line in f.readlines()]
        return all(lines)
    return True

if __name__ == '__main__':
    numtests = 1
    if len(sys.argv) == 2:
        numTests = sys.argv[1]

    main(numTests)
