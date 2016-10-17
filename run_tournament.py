import os

Players = ["g1","g2","g3","g4","g5","g6","g8","g9","g10"]
Sequencers = ["g1","g2","g3","g4","g5","g6","g8","g9","g10","random","tailheavy","starsandblocks","industrialization","misfits"]
Seeds = [ 400,   260,   431,   264,   146]


for sequencer in Sequencers:
    for player in Players:
        for seed in Seeds:
            command = "java pentos.sim.Simulator -g " + player + " -s " + sequencer + " -i " + str(seed) + " 2>> " + sequencer + ".txt"
            print command
            os.system(command)    
