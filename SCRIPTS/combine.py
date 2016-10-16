#combine csv files containing outputs for individual building distributions into one csv file containing everything
import csv

Sequencers = ["g1","g2","g3","g4","g5","g6","g8","g9","g10","random","tailheavy","starsandblocks","industrialization","misfits"]
netresults = [[1], [2], [3], [4], [5], [6], [8], [9], [10]]

for name in Sequencers:    
    with open(name + ".csv") as f:
        csvf = csv.reader(f)
        results = []
        for row in csvf:
            results.append(row)
            
        for i in range(9):
            netresults[i].extend(results[i])


with open("results.csv",'w') as output:
    w = csv.writer(output)
    for row in netresults:
        w.writerow(row)

