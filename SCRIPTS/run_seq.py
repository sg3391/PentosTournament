#run simulator over a single building distribution whose name is passed in as a command line argument
#usage: python run_seq.py <name_of_distribution>

import os
import sys

sequencer = "";
for arg in sys.argv:
    sequencer = arg
Players = ["g1","g2","g3","g4","g5","g6","g8","g9","g10"]
Seeds = [869,    84,   400,   260,   800,   431,   911,   182,   264,   146];


for player in Players:
    for seed in Seeds:
        command = "java pentos.sim.Simulator -g " + player + " -s " + sequencer + " -i " + str(seed) + " 2>> " + sequencer + ".txt"
        print command
        os.system(command)    
