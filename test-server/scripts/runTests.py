#!/usr/bin/python
import os
import sys
import time
from os import path
from subprocess import call

def main(numTests, scheduler, period, depth):
    os.chdir("..")

    if len(scheduler.split(".")) <= 0:
        print("Please enter a valid scheduler name: e.g. explorer.scheduler.NodeFailureInjector")
        return

    resultFile = "result" + scheduler.split(".")[-1] + "P" + period + "D" + depth
    print(resultFile)
    start_all = time.time()
    for i in range(1, int(numTests)+1):
        print("Running test %s" % i)
        #startB = time.time()

        seed = i + 12345678
        #seed = 12346496
        call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=explorer.SystemRunner", "-Dexec.args=\"scheduler={0} randomSeed={1} linkEstablishmentPeriod={2} resultFile={3} bugDepth={4}\" ".format(scheduler, str(seed), str(period), resultFile, depth)), shell=True)

        #endB = time.time()
        #elapsedSec = endB - startB
        #print("All Seconds for test %s: %s" % (i, elapsedSec))
        #print("All Minutes for test %s: %s" % (i, elapsedSec / 60))

    endAll = time.time()
    elapsedSec = endAll - start_all
    secs = "All Seconds for all tests: {}".format(elapsedSec)
    mins = "All Minutes for all tests: {}".format(elapsedSec / 60)

    print(secs)
    print(mins)

    f = open(resultFile, 'a')
    f.write(secs + "\n" + mins + "\n")
    f.close()


if __name__ == '__main__':
     numtests = 1
     if len(sys.argv) != 5:
        print("Please enter the parameters for: numtests, scheduler, period and depth")
        print("Example usage: python runtests.py 10 explorer.scheduler.NodeFailureInjector 6 6 ")
     else:
        main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
