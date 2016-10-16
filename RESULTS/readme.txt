results.csv contains the results over all building distributions. There are 9 rows, one for each group number, in numeric order (1,2,3,4,5,6,8,9,10). Each consecutive block of 10 columns is one building distribution. The distributions are ordered by the array in the run_tournament.py file

Sequencers = ["g1","g2","g3","g4","g5","g6","g8","g9","g10","random","tailheavy","starsandblocks","industrialization","misfits"]

Within each block of 10 columns are 10 runs using different seeds. The seeds used are the following, so that you can reproduce results if needed:

Seeds = [869,    84,   400,   260,   800,   431,   911,   182,   264,   146]

Within the individual folder, there are csv files containing results run over each distribution individually, named by the respective distribution name. These have 9 rows by 10 columns, one row per player and one column per seed, following the same ordering.

Note that some players threw exceptions or timed out. A score of -1 means the player timed out (300 seconds) or used more than (something like) 12GB of RAM and caused the simulation to crash on the Google Cloud. A score of -2 means the player threw an exception (e.g. placed a building not next to a road). All other scores means the player finished successfully.
