#import csv
import numpy as np
import sys
import math

dirname=sys.argv[1]
infilename=dirname+"/input_original.dat"
outfilename=dirname+"/input.dat"
y_rmsfilename="y_rms.dat"

noiselevel = 0.01
npoints = 10

# read input file
header = []
body = []
i = 0
for line in open(infilename):
    if i < 2:
        header.append(line)
    elif i < npoints + 2:
        listwords = line.split()
        body.append(listwords)
    else:
        break
    i += 1

# compute rms
n = len(body)
y = [0.0] * n
sum = 0.0
for i in range(n):
    temp = float(body[i][-1])
    sum += temp**2
    y[i] = temp
y_rms = math.sqrt(sum / n)

# create noise
mu = 0.0
sigma = noiselevel * y_rms
sample = np.random.normal(mu, sigma, n)
print("sigma = ", sigma)
print("sample = ",sample)

# add noise to y values
for i in range(n):
    temp = y[i] + sample[i]
    body[i][-1] = str(temp)


# write output file
print(header[0])
print(header[1])

for x in header:
    print(x)

for x in body:
    print(x)

f = open(outfilename, "x")
f.write(header[0])
f.write(header[1])
for x in body:
    temp = ' '.join(x)
    temp += ' '
    temp += '\n'
    f.write(temp)
f.close()

# write y_rms file
f = open(y_rmsfilename, "a")
temp = dirname + " " + str(y_rms) + '\n'
f.write(temp)
f.close()

