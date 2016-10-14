import os

Players = ["g1","g2","g3","g4","g5","g6","g8","g9","g10"]
Sequencers = ["g1","g2","g3","g4","g5","g6","g8","g9","g10","random","tailheavy","starsandblocks","industrialization","misfits"]
Seeds = [869,    84,   400,   260,   800,   431,   911,   182,   264,   146,   136,   869,   580,   550,   145,   853,   622,   351,   513,   402,    76,   240,   123,   184,   240]


for sequencer in Sequencers:
    for player in Players:
        for seed in Seeds:
            command = "java pentos.sim.Simulator -g " + player + " -s " + sequencer + " -i " + str(seed) + " 2>> " + sequencer + ".txt"
            os.system(command)    
