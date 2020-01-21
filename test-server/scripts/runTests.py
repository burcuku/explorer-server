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
        #startB = time.time()

        seed = i + 12345678
        #seed = 12346496
        call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=explorer.SystemRunner", "-Dexec.args=\"randomSeed={0} linkEstablishmentPeriod={1}\" ".format(str(seed), str(88))), shell=True)

        #endB = time.time()
        #elapsedSec = endB - startB
        #print("All Seconds for test %s: %s" % (i, elapsedSec))
        #print("All Minutes for test %s: %s" % (i, elapsedSec / 60))

    endAll = time.time()
    elapsedSec = endAll - start_all
    print("All Seconds for all tests: %s" % elapsedSec)
    print("All Minutes for all tests: %s" % (elapsedSec / 60))

    # create mutations
    #call("mvn {0} {1}".format("exec:java", "-Dexec.mainClass=explorer.MutatorCreatorMain"), shell=True)

#     startM = time.time()
#     # run mutations
#     mutationNo = 1
#     # read mutations file and print them:
#     f = open("mutations", 'r')
#     for line in f.readlines():
#         if len(line.strip()) != 0 :
#             print("Running mutation %s" % mutationNo)
#             mutation = line.rstrip('\n')
#             #print mutation
#             call("mvn {0} {1} {2}".format("exec:java", "-Dexec.mainClass=explorer.SystemRunner", "-Dexec.args=\"seed {0} failures {1}\"".format(str(seed), mutation)), shell=True)
#             mutationNo = mutationNo + 1
#
#     endM = time.time()
#     elapsedM = endM - startM
#     elapsedSec2 = endM - start_all
#     print("All Seconds for mutations: %s" % elapsedM)
#     print("All Seconds for all: %s" % elapsedSec2)

# def isFileEmpty(filename):
#     with open(filename, 'r') as f:
#         lines = [len(line.rstrip('\n')) == 0 for line in f.readlines()]
#         return all(lines)
#     return True

if __name__ == '__main__':
     numtests = 1
     if len(sys.argv) == 2:
         numTests = sys.argv[1]
     main(numTests)
