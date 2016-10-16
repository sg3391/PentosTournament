# open txt file of simulator outputs for one building distribution and write a csv file with a line per player
import sys
import csv

filename = ""
for arg in sys.argv:
    filename = arg

results = []

linenum = 0;
presults = [0 for i in range(10)]
with open(filename + ".txt") as f:
    for line in f:            
        tokens = line.split(" ")
        if len(tokens) == 4:
            presults[linenum % 10] = int(tokens[3])
            linenum+=1
            if linenum % 10 == 0:
                results.append(presults)
                presults = [0 for i in range(10)]
            
with open(filename + ".csv",'w') as output:
    w = csv.writer(output)
    for row in results:
        w.writerow(row)
