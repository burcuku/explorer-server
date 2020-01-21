import os
import sys
from os import path

S_ROUNDS_STR = 'Num successful rounds: '
ROUNDS_STR = 'Num rounds: '
S_PHASES_STR = 'Num successful phases: '
PHASES_STR = 'Num phases: '
MSGS_STR = 'Num messages: '


def calculate_stats(num_rounds, num_suc_rounds, num_suc_phases, num_phases, num_msgs):
    stats = {}
    stats["rounds"] = {
        "average": sum(num_rounds) / len(num_rounds) if num_rounds else "N/A",
        "count": len(num_rounds)
    }
    stats["successful_rounds"] = {
        "average": sum(num_suc_rounds) / len(num_suc_rounds) if num_suc_rounds else "N/A",
        "count": len(num_suc_rounds)
    }
    stats["phases"] = {
        "average": sum(num_phases) / len(num_phases) if num_phases else "N/A",
        "count": len(num_phases)
    }
    stats["successful_phases"] = {
        "average": sum(num_suc_phases) / len(num_suc_phases) if num_suc_phases else "N/A",
        "count": len(num_suc_phases)
    }
    stats["messages"] = {
         "average": sum(num_msgs) / len(num_msgs) if num_msgs else "N/A",
         "count": len(num_msgs)
    }
    return stats

def process_file(stat_file):
    num_rounds = []
    num_suc_rounds = []
    num_phases = []
    num_suc_phases = []
    num_msgs = []

    with open(stat_file, "r") as stats:
        for line in stats:
            if line.startswith(S_ROUNDS_STR):
                v = int(line[len(S_ROUNDS_STR):])
                num_suc_rounds.append(v)
            elif line.startswith(ROUNDS_STR):
                v = int(line[len(ROUNDS_STR):])
                num_rounds.append(v)
            elif line.startswith(S_PHASES_STR):
                v = int(line[len(S_PHASES_STR):])
                num_suc_phases.append(v)
            elif line.startswith(PHASES_STR):
                v = int(line[len(PHASES_STR):])
                num_phases.append(v)
            elif line.startswith(MSGS_STR):
                v = int(line[len(MSGS_STR):])
                num_msgs.append(v)

    return calculate_stats(num_rounds, num_suc_rounds, num_suc_phases, num_phases, num_msgs)


if __name__ == '__main__':
    stats = process_file(sys.argv[1])
    print(stats)
